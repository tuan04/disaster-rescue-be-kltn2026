package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.grpc.UserInfoGrpcResponse;
import iuh.fit.resourcemanagementservice.client.UserGrpcClient;
import iuh.fit.resourcemanagementservice.dtos.request.AddMemberRequest;
import iuh.fit.resourcemanagementservice.dtos.request.AddMembersRequest;
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
import iuh.fit.resourcemanagementservice.mappers.CampaignMemberMapper;
import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus;
import iuh.fit.resourcemanagementservice.enums.MembershipType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    @Transactional(readOnly = true)
    public boolean hasActiveAcceptedMembership(UUID memberId) {
        if (memberId == null) {
            return false;
        }
        return campaignMemberRepository.existsActiveAcceptedMembership(memberId);
    }

    public List<MemberResponse> addMembers(UUID teamId, UUID currentUserId, AddMembersRequest request) {
        CampaignTeam team = findTeamById(teamId);

        if (!team.getLeaderId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Chỉ Đội trưởng mới có quyền mời thành viên vào đội");
        }

        if (team.getStatus() == TeamStatus.DISBANDED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Đội cứu hộ đã giải tán");
        }
        if (team.getApprovalStatus() != TeamApprovalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Đội cứu hộ chưa được phê duyệt hoạt động");
        }
        if (team.getCampaign().getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "Chiến dịch cứu hộ không còn hoạt động");
        }

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

        return transactionTemplate.execute(status -> {
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

            List<CampaignMember> savedMembers = campaignMemberRepository.saveAll(entitiesToSave);
            return savedMembers.stream().map(campaignMemberMapper::toResponse).toList();
        });
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
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getTeamMembers(UUID teamId) {
        findTeamById(teamId);

        return campaignMemberRepository.findActiveMembersByTeamId(teamId)
                .stream()
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

        String lockKey = "lock:rescuer:membership:" + rescuerId;
        boolean acquired = redisService.acquireLock(lockKey, 5);
        if (!acquired) {
            throw new BusinessException(ErrorCode.CONFLICT, "Đã gửi yêu cầu chấp nhận lời mời");
        }

        try {
            return transactionTemplate.execute(status -> {
                CampaignMember member = campaignMemberRepository.findByIdWithTeamAndCampaign(membershipId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Lời mời tham gia đội không tồn tại"));

                if (!rescuerId.equals(member.getMemberId()) || member.getMembershipStatus() != MembershipStatus.INVITED) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "Không thể thao tác với lời mời này");
                }

                CampaignTeam team = member.getCampaignTeam();
                if (team.getStatus() == TeamStatus.DISBANDED) {
                    throw new BusinessException(ErrorCode.CONFLICT, "Đội cứu hộ đã giải tán");
                }
                if (team.getApprovalStatus() != TeamApprovalStatus.APPROVED) {
                    throw new BusinessException(ErrorCode.CONFLICT, "Đội cứu hộ chưa được phê duyệt hoạt động");
                }
                if (team.getCampaign().getStatus() != CampaignStatus.ACTIVE) {
                    throw new BusinessException(ErrorCode.CONFLICT, "Chiến dịch cứu hộ không còn hoạt động");
                }

                if (campaignMemberRepository.existsActiveAcceptedMembership(rescuerId)) {
                    throw new BusinessException(ErrorCode.CONFLICT, "Bạn đã tham gia một đội cứu hộ khác");
                }

                member.setMembershipStatus(MembershipStatus.ACCEPTED);
                CampaignMember saved = campaignMemberRepository.save(member);

                campaignMemberRepository.cancelOtherPendingInvitations(rescuerId, membershipId);

                return campaignMemberMapper.toResponse(saved);
            });
        } finally {
            redisService.releaseLock(lockKey);
        }
    }

    @Transactional
    public MemberResponse declineInvitation(UUID membershipId, UUID rescuerId) {
        if (rescuerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu thông tin người dùng");
        }

        CampaignMember member = campaignMemberRepository.findById(membershipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Lời mời tham gia đội không tồn tại"));

        if (!rescuerId.equals(member.getMemberId()) || member.getMembershipStatus() != MembershipStatus.INVITED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Không thể thao tác với lời mời này");
        }

        member.setMembershipStatus(MembershipStatus.DECLINED);
        CampaignMember saved = campaignMemberRepository.save(member);

        return campaignMemberMapper.toResponse(saved);
    }

    private CampaignTeam findTeamById(UUID teamId) {
        return campaignTeamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đội cứu hộ với ID: " + teamId
                ));
    }
}
