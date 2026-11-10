package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.grpc.UserInfoGrpcResponse;
import iuh.fit.resourcemanagementservice.client.UserGrpcClient;
import iuh.fit.resourcemanagementservice.dtos.request.AddMemberRequest;
import iuh.fit.resourcemanagementservice.dtos.request.AddMembersRequest;
import iuh.fit.resourcemanagementservice.dtos.request.JoinTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.response.MemberResponse;
import iuh.fit.resourcemanagementservice.dtos.response.TeamInvitationResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.SexEnum;
import iuh.fit.resourcemanagementservice.enums.TeamRole;
import iuh.fit.resourcemanagementservice.enums.MembershipStatus;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import iuh.fit.resourcemanagementservice.repositories.CampaignMemberRepository;
import iuh.fit.resourcemanagementservice.repositories.CampaignTeamRepository;
import iuh.fit.common.kafka.dto.TeamMemberEventType;
import iuh.fit.resourcemanagementservice.events.producer.TeamEventProducer;
import iuh.fit.resourcemanagementservice.mappers.CampaignMemberMapper;
import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus;
import iuh.fit.resourcemanagementservice.enums.MembershipType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignMemberService {

    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignTeamRepository campaignTeamRepository;
    private final CampaignMemberMapper campaignMemberMapper;
    private final UserGrpcClient userGrpcClient;
    private final RedisService redisService;
    private final TeamEventProducer teamEventProducer;

    private final TransactionTemplate transactionTemplate;

    @Transactional
    public void createLeaderMember(CampaignTeam team, UserInfoGrpcResponse leaderInfo) {
        CampaignMember leaderMember = campaignMemberMapper.toMember(
                team,
                team.getLeaderId(),
                leaderInfo.getFullName(),
                team.getLeaderPhone(),
                leaderInfo.getAvatarUrl(),
                SexEnum.from(leaderInfo.getSex()),
                TeamRole.LEADER,
                MembershipType.REGISTERED,
                MembershipStatus.ACCEPTED
        );
        campaignMemberRepository.save(leaderMember);
    }

    @Transactional
    public void cancelLeaderMembership(CampaignTeam team) {
        CampaignMember leaderMember = campaignMemberRepository
                .findByCampaignTeam_IdAndMemberId(team.getId(), team.getLeaderId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy trưởng đội trong danh sách thành viên"));
        leaderMember.setMembershipStatus(MembershipStatus.CANCELLED);
    }

    @Transactional
    public CampaignMember transferLeaderRole(UUID teamId, UUID currentLeaderId, UUID newLeaderId) {
        CampaignMember newLeaderMember = campaignMemberRepository.findByCampaignTeam_IdAndMemberId(teamId, newLeaderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Thành viên không thuộc đội"));
        if (newLeaderMember.getMembershipStatus() != MembershipStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Chỉ có thể chuyển giao cho thành viên chính thức");
        }

        CampaignMember oldLeaderMember = campaignMemberRepository.findByCampaignTeam_IdAndMemberId(teamId, currentLeaderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin Đội trưởng hiện tại trong đội"));

        oldLeaderMember.setRoleInTeam(TeamRole.MEMBER);
        newLeaderMember.setRoleInTeam(TeamRole.LEADER);
        campaignMemberRepository.save(oldLeaderMember);
        return campaignMemberRepository.save(newLeaderMember);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveAcceptedMembership(UUID memberId) {
        if (memberId == null) {
            return false;
        }
        return campaignMemberRepository.existsActiveAcceptedMembership(memberId);
    }


    @Transactional(readOnly = true)
    public Optional<CampaignTeam> findActiveTeamByMemberId(UUID memberId) {
        if (memberId == null) {
            return Optional.empty();
        }
        return campaignMemberRepository.findActiveTeamByMemberId(memberId);
    }

    public List<MemberResponse> addMembers(UUID teamId, UUID currentUserId, AddMembersRequest request) {
        CampaignTeam team = findTeamById(teamId);

        if (!team.getLeaderId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ Đội trưởng mới có quyền mời thành viên vào đội");
        }

        validateTeamAndCampaignActive(team);

        Set<UUID> memberIds = new HashSet<>();
        Set<String> guestPhones = new HashSet<>();

        for (AddMemberRequest mReq : request.members()) {
            if (mReq == null) continue;
            if (mReq.memberId() != null) {
                memberIds.add(mReq.memberId());
            } else {
                guestPhones.add(mReq.phone().trim());
            }
        }

        if (!guestPhones.isEmpty()) {
            List<String> existingPhones = campaignMemberRepository.findExistingPhonesInTeam(teamId, guestPhones);
            if (!existingPhones.isEmpty()) {
                throw new BusinessException(ErrorCode.CONFLICT, "Số điện thoại khách " + String.join(", ", existingPhones) + " đã tồn tại trong đội này");
            }
        }

        Map<UUID, CampaignMember> existingMemberMap = Map.of();
        if (!memberIds.isEmpty()) {
            List<CampaignMember> existingMembers = campaignMemberRepository.findByCampaignTeam_IdAndMemberIdIn(teamId, memberIds);
            existingMemberMap = existingMembers.stream().collect(Collectors.toMap(CampaignMember::getMemberId, m -> m));

            for (CampaignMember existing : existingMembers) {
                if (existing.getMembershipStatus() == MembershipStatus.ACCEPTED) {
                    throw new BusinessException(ErrorCode.CONFLICT, "Người dùng " + existing.getFullName() + " đã là thành viên chính thức của đội");
                }
                if (existing.getMembershipStatus() == MembershipStatus.INVITED) {
                    throw new BusinessException(ErrorCode.CONFLICT, "Người dùng " + existing.getFullName() + " đã được mời vào đội và đang chờ phản hồi");
                }
            }

            List<UUID> activeAcceptedIds = campaignMemberRepository.findActiveAcceptedMemberIds(memberIds);
            if (!activeAcceptedIds.isEmpty()) {
                throw new BusinessException(ErrorCode.CONFLICT, "Thành viên hiện đang tham gia một đội cứu hộ khác đang hoạt động");
            }
        }

        Map<UUID, UserInfoGrpcResponse> userMap = memberIds.isEmpty() ? Map.of() : userGrpcClient.getUsersByIds(memberIds);

        for (UUID memberId : memberIds) {
            UserInfoGrpcResponse userInfo = userMap.get(memberId);
            if (userInfo == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin người dùng " + memberId);
            }
            if (userInfo.getRole() == null || !userInfo.getRole().equalsIgnoreCase("RESCUER")) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Người dùng " + userInfo.getFullName() + " không phải là cứu hộ viên");
            }
        }

        final Map<UUID, CampaignMember> finalExistingMap = existingMemberMap;

        List<CampaignMember> savedMembers = transactionTemplate.execute(status -> {
            List<CampaignMember> entitiesToSave = new ArrayList<>();

            for (AddMemberRequest mReq : request.members()) {
                if (mReq == null) continue;
                if (mReq.memberId() == null) {
                    entitiesToSave.add(campaignMemberMapper.toMember(
                            team, null, mReq.fullName(), mReq.phone(), null, mReq.sex(),
                            mReq.roleInTeam(), MembershipType.GUEST, MembershipStatus.ACCEPTED
                    ));
                } else {
                    UserInfoGrpcResponse userInfo = userMap.get(mReq.memberId());
                    CampaignMember existing = finalExistingMap.get(mReq.memberId());

                    if (existing != null) {
                        entitiesToSave.add(campaignMemberMapper.updateMember(
                                existing, userInfo.getFullName(), userInfo.getPhoneNumber(), userInfo.getAvatarUrl(),
                                SexEnum.from(userInfo.getSex()), mReq.roleInTeam(), MembershipStatus.INVITED
                        ));
                    } else {
                        entitiesToSave.add(campaignMemberMapper.toMember(
                                team, mReq.memberId(), userInfo.getFullName(), userInfo.getPhoneNumber(), userInfo.getAvatarUrl(),
                                SexEnum.from(userInfo.getSex()), mReq.roleInTeam(), MembershipType.REGISTERED, MembershipStatus.INVITED
                        ));
                    }
                }
            }

            return campaignMemberRepository.saveAll(entitiesToSave);
        });

        if (savedMembers != null) {
            List<UUID> invitedUserIds = savedMembers.stream()
                    .filter(m -> m.getMemberId() != null && m.getMembershipStatus() == MembershipStatus.INVITED)
                    .map(CampaignMember::getMemberId)
                    .toList();
            teamEventProducer.sendTeamEventToUsers(
                    team,
                    TeamMemberEventType.INVITED,
                    invitedUserIds,
                    currentUserId,
                    team.getLeaderName(),
                    String.format("Đội trưởng %s đã mời bạn gia nhập đội cứu hộ \"%s\"", team.getLeaderName(), team.getTeamName())
            );
        }

        return savedMembers == null ? List.of() : savedMembers.stream().map(campaignMemberMapper::toResponse).toList();
    }

    @Transactional
    public void removeMember(UUID teamId, UUID currentUserId, UUID memberId) {
        CampaignTeam team = findTeamById(teamId);

        if (currentUserId == null || !team.getLeaderId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Không có quyền thực hiện thao tác này");
        }

        if (team.getStatus() == TeamStatus.BUSY) {
            throw new BusinessException(ErrorCode.CONFLICT, "Không thể xóa thành viên khi đội đang làm nhiệm vụ");
        }

        if (team.getStatus() == TeamStatus.DISBANDED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Đội cứu hộ đã giải tán");
        }

        CampaignMember member = campaignMemberRepository.findByIdAndCampaignTeam_Id(memberId, teamId)
        .orElseThrow(() -> new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "Thành viên không tồn tại trong đội này"
        ));

        if (member.getRoleInTeam() == TeamRole.LEADER) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Không thể xóa Đội trưởng khỏi đội cứu hộ."
            );
        }

        if (member.getMembershipStatus() == MembershipStatus.CANCELLED || member.getMembershipStatus() == MembershipStatus.DECLINED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Thành viên này hiện không còn trong đội");
        }

        member.setMembershipStatus(MembershipStatus.CANCELLED);
        campaignMemberRepository.save(member);

        teamEventProducer.sendTeamEvent(
                team,
                TeamMemberEventType.MEMBER_REMOVED,
                member.getMemberId(),
                currentUserId,
                team.getLeaderName(),
                String.format("Bạn đã được mời rời khỏi Đội cứu hộ \"%s\".", team.getTeamName())
        );
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getTeamMembers(UUID teamId, MembershipStatus status) {
        findTeamById(teamId);

        List<CampaignMember> members;
        if (status != null) {
            members = campaignMemberRepository.findMembersByTeamIdAndStatus(teamId, status);
        } else {
            members = campaignMemberRepository.findActiveMembersByTeamId(teamId);
        }

        return members.stream()
                .map(campaignMemberMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponse> getMyInvitations(UUID rescuerId) {
        if (rescuerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu thông tin người dùng");
        }

        List<CampaignMember> invitations = campaignMemberRepository.findMyInvitations(rescuerId);
        return invitations.stream()
                .map(TeamInvitationResponse::fromEntity)
                .toList();
    }

    public MemberResponse acceptInvitation(UUID membershipId, UUID rescuerId) {
        if (rescuerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu thông tin người dùng");
        }

        String lockKey = RedisService.RESCUER_MEMBERSHIP_LOCK_KEY + rescuerId;
        String lockToken = redisService.acquireLock(lockKey, 5);
        if (lockToken == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "Đã gửi yêu cầu chấp nhận lời mời");
        }

        try {
            CampaignMember saved = transactionTemplate.execute(status -> {
                CampaignMember member = campaignMemberRepository.findByIdWithTeamAndCampaign(membershipId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Lời mời tham gia đội không tồn tại"));

                if (!rescuerId.equals(member.getMemberId()) || member.getMembershipStatus() != MembershipStatus.INVITED) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "Không thể thao tác với lời mời này");
                }

                CampaignTeam team = member.getCampaignTeam();
                validateTeamAndCampaignActive(team);
                validateRescuerAvailability(rescuerId, team);

                member.setMembershipStatus(MembershipStatus.ACCEPTED);
                CampaignMember result = campaignMemberRepository.save(member);

                campaignMemberRepository.cancelOtherPendingInvitations(rescuerId, membershipId);

                return result;
            });

            if (saved != null) {
                teamEventProducer.sendTeamEvent(
                        saved.getCampaignTeam(),
                        TeamMemberEventType.INVITATION_ACCEPTED,
                        saved.getCampaignTeam().getLeaderId(),
                        rescuerId,
                        saved.getFullName(),
                        String.format("%s đã chấp nhận lời mời và gia nhập đội \"%s\".", saved.getFullName(), saved.getCampaignTeam().getTeamName())
                );
            }

            return campaignMemberMapper.toResponse(saved);
        } finally {
            redisService.releaseLock(lockKey, lockToken);
        }
    }

    @Transactional
    public MemberResponse declineInvitation(UUID membershipId, UUID rescuerId) {
        if (rescuerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu thông tin người dùng");
        }

        CampaignMember member = campaignMemberRepository.findByIdWithTeamAndCampaign(membershipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Lời mời tham gia đội không tồn tại"));

        if (!rescuerId.equals(member.getMemberId()) || member.getMembershipStatus() != MembershipStatus.INVITED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Không thể thao tác với lời mời này");
        }

        member.setMembershipStatus(MembershipStatus.DECLINED);
        CampaignMember saved = campaignMemberRepository.save(member);

        teamEventProducer.sendTeamEvent(
                saved.getCampaignTeam(),
                TeamMemberEventType.INVITATION_DECLINED,
                saved.getCampaignTeam().getLeaderId(),
                rescuerId,
                saved.getFullName(),
                String.format("%s đã từ chối lời mời gia nhập đội \"%s\".", saved.getFullName(), saved.getCampaignTeam().getTeamName())
        );

        return campaignMemberMapper.toResponse(saved);
    }

    /**
     * Cứu hộ viên gửi yêu cầu xin gia nhập đội cứu hộ
     */
    @Transactional
    public MemberResponse requestToJoinTeam(UUID teamId, UUID rescuerId, JoinTeamRequest request) {
        if (rescuerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu thông tin người dùng");
        }

        CampaignTeam team = findTeamById(teamId);

        validateTeamAndCampaignActive(team);
        validateRescuerAvailability(rescuerId, team);

        if (request == null || request.roleInTeam() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Vai trò trong đội không được để trống");
        }
        TeamRole role = request.roleInTeam();
        if (role == TeamRole.LEADER) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Không thể xin gia nhập với vai trò Đội trưởng");
        }

        UserInfoGrpcResponse userInfo = userGrpcClient.getUserById(rescuerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin người dùng"));

        if (userInfo.getRole() == null || !userInfo.getRole().equalsIgnoreCase("RESCUER")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Chỉ cứu hộ viên mới có quyền xin gia nhập đội cứu hộ");
        }

        Optional<CampaignMember> existingOpt = campaignMemberRepository.findByCampaignTeam_IdAndMemberId(teamId, rescuerId);
        CampaignMember memberToSave;

        if (existingOpt.isPresent()) {
            CampaignMember existing = existingOpt.get();
            if (existing.getMembershipStatus() == MembershipStatus.ACCEPTED) {
                throw new BusinessException(ErrorCode.CONFLICT, "Bạn đã là thành viên chính thức của đội này");
            }
            if (existing.getMembershipStatus() == MembershipStatus.REQUESTED) {
                throw new BusinessException(ErrorCode.CONFLICT, "Bạn đã gửi yêu cầu gia nhập đội này rồi");
            }
            if (existing.getMembershipStatus() == MembershipStatus.INVITED) {
                throw new BusinessException(ErrorCode.CONFLICT, "Bạn đã được đội này mời tham gia");
            }

            memberToSave = campaignMemberMapper.updateMember(
                    existing,
                    userInfo.getFullName(),
                    userInfo.getPhoneNumber(),
                    userInfo.getAvatarUrl(),
                    SexEnum.from(userInfo.getSex()),
                    role,
                    MembershipStatus.REQUESTED
            );
        } else {
            memberToSave = campaignMemberMapper.toMember(
                    team,
                    rescuerId,
                    userInfo.getFullName(),
                    userInfo.getPhoneNumber(),
                    userInfo.getAvatarUrl(),
                    SexEnum.from(userInfo.getSex()),
                    role,
                    MembershipType.REGISTERED,
                    MembershipStatus.REQUESTED
            );
        }

        CampaignMember saved = campaignMemberRepository.save(memberToSave);

        teamEventProducer.sendTeamEvent(
                team,
                TeamMemberEventType.JOIN_REQUESTED,
                team.getLeaderId(),
                rescuerId,
                saved.getFullName(),
                String.format("%s đã gửi yêu cầu gia nhập đội cứu hộ \"%s\".", saved.getFullName(), team.getTeamName())
        );

        return campaignMemberMapper.toResponse(saved);
    }

    /**
     * Cứu hộ viên hủy yêu cầu xin gia nhập đội cứu hộ
     */
    @Transactional
    public MemberResponse cancelJoinRequest(UUID membershipId, UUID rescuerId) {
        if (rescuerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu thông tin người dùng");
        }

        CampaignMember member = campaignMemberRepository.findById(membershipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Yêu cầu gia nhập đội không tồn tại"));

        if (!rescuerId.equals(member.getMemberId()) || member.getMembershipStatus() != MembershipStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Không thể thao tác với yêu cầu này");
        }

        member.setMembershipStatus(MembershipStatus.CANCELLED);
        CampaignMember saved = campaignMemberRepository.save(member);

        return campaignMemberMapper.toResponse(saved);
    }

    /**
     * Đội trưởng phê duyệt yêu cầu xin gia nhập đội
     */
    public MemberResponse approveJoinRequest(UUID membershipId, UUID leaderId) {
        if (leaderId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu thông tin người dùng");
        }

        CampaignMember member = campaignMemberRepository.findByIdWithTeamAndCampaign(membershipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Yêu cầu gia nhập đội không tồn tại"));

        String lockKey = RedisService.RESCUER_MEMBERSHIP_LOCK_KEY + member.getMemberId();
        String lockToken = redisService.acquireLock(lockKey, 5);
        if (lockToken == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "Đang duyệt yêu cầu gia nhập");
        }

        try {
            CampaignMember saved = transactionTemplate.execute(status -> {
                CampaignTeam team = member.getCampaignTeam();
                if (!leaderId.equals(team.getLeaderId())) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ Đội trưởng mới có quyền duyệt yêu cầu gia nhập");
                }

                if (member.getMembershipStatus() != MembershipStatus.REQUESTED) {
                    throw new BusinessException(ErrorCode.CONFLICT, "Yêu cầu gia nhập không ở trạng thái chờ duyệt");
                }

                validateTeamAndCampaignActive(team);
                validateRescuerAvailability(member.getMemberId(), team);

                member.setMembershipStatus(MembershipStatus.ACCEPTED);
                CampaignMember result = campaignMemberRepository.save(member);

                campaignMemberRepository.cancelOtherPendingInvitations(member.getMemberId(), membershipId);

                return result;
            });

            if (saved != null) {
                teamEventProducer.sendTeamEvent(
                        saved.getCampaignTeam(),
                        TeamMemberEventType.JOIN_APPROVED,
                        saved.getMemberId(),
                        leaderId,
                        saved.getCampaignTeam().getLeaderName(),
                        String.format("Yêu cầu gia nhập đội cứu hộ \"%s\" của bạn đã được phê duyệt!", saved.getCampaignTeam().getTeamName())
                );
            }

            return campaignMemberMapper.toResponse(saved);
        } finally {
            redisService.releaseLock(lockKey, lockToken);
        }
    }

    /**
     * Đội trưởng từ chối yêu cầu xin gia nhập đội
     */
    @Transactional
    public MemberResponse rejectJoinRequest(UUID membershipId, UUID leaderId) {
        if (leaderId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu thông tin người dùng");
        }

        CampaignMember member = campaignMemberRepository.findByIdWithTeamAndCampaign(membershipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Yêu cầu gia nhập không tồn tại"));

        CampaignTeam team = member.getCampaignTeam();
        if (!leaderId.equals(team.getLeaderId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ Đội trưởng mới có quyền từ chối yêu cầu gia nhập");
        }

        if (member.getMembershipStatus() != MembershipStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Yêu cầu gia nhập này không ở trạng thái chờ duyệt");
        }

        member.setMembershipStatus(MembershipStatus.DECLINED);
        CampaignMember saved = campaignMemberRepository.save(member);

        teamEventProducer.sendTeamEvent(
                saved.getCampaignTeam(),
                TeamMemberEventType.JOIN_REJECTED,
                saved.getMemberId(),
                leaderId,
                saved.getCampaignTeam().getLeaderName(),
                String.format("Yêu cầu gia nhập đội cứu hộ \"%s\" của bạn đã bị từ chối.", saved.getCampaignTeam().getTeamName())
        );

        return campaignMemberMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UUID> getAcceptedMemberUserIds(UUID teamId) {
        return campaignMemberRepository.findMembersByTeamIdAndStatus(teamId, MembershipStatus.ACCEPTED)
                .stream()
                .map(CampaignMember::getMemberId)
                .filter(Objects::nonNull)
                .toList();
    }

    private CampaignTeam findTeamById(UUID teamId) {
        return campaignTeamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đội cứu hộ với ID: " + teamId
                ));
    }

    private void validateTeamAndCampaignActive(CampaignTeam team) {
        if (team.getStatus() == TeamStatus.DISBANDED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Đội cứu hộ đã giải tán");
        }
        if (team.getApprovalStatus() != TeamApprovalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Đội cứu hộ chưa được phê duyệt hoạt động");
        }
        if (team.getCampaign().getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "Chiến dịch cứu hộ không còn hoạt động");
        }
    }

    private void validateRescuerAvailability(UUID rescuerId, CampaignTeam team) {
        if (team.getLeaderId().equals(rescuerId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cứu hộ viên đang là Đội trưởng của đội này");
        }
        if (campaignMemberRepository.existsActiveAcceptedMembership(rescuerId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cứu hộ viên đã tham gia một đội cứu hộ khác đang hoạt động");
        }
    }
}
