package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.grpc.UserInfoGrpcResponse;
import iuh.fit.resourcemanagementservice.client.UserGrpcClient;
import iuh.fit.resourcemanagementservice.dtos.request.AddMemberRequest;
import iuh.fit.resourcemanagementservice.dtos.request.AddMembersRequest;
import iuh.fit.resourcemanagementservice.dtos.response.MemberResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.SexEnum;
import iuh.fit.resourcemanagementservice.enums.TeamRole;
import iuh.fit.resourcemanagementservice.enums.MembershipStatus;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import iuh.fit.resourcemanagementservice.repositories.CampaignMemberRepository;
import iuh.fit.resourcemanagementservice.repositories.CampaignTeamRepository;
import iuh.fit.resourcemanagementservice.mappers.CampaignMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignMemberService {

    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignTeamRepository campaignTeamRepository;
    private final CampaignMemberMapper campaignMemberMapper;
    private final UserGrpcClient userGrpcClient;

    @Transactional
    public void createLeaderMember(CampaignTeam team, UserInfoGrpcResponse leaderInfo) {
        CampaignMember leaderMember = campaignMemberMapper.toLeaderMember(
                team,
                team.getLeaderId(),
                team.getLeaderPhone(),
                leaderInfo.getFullName(),
                leaderInfo.getAvatarUrl(),
                SexEnum.from(leaderInfo.getSex())
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

    @Transactional
    public List<MemberResponse> addMembers(UUID teamId, AddMembersRequest request) {
        CampaignTeam team = findTeamById(teamId);

        if (team.getStatus() == TeamStatus.DISBANDED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Đội cứu hộ đã giải tán, không thể thêm thành viên mới"
            );
        }

        Set<UUID> seenMemberIds = new HashSet<>();
        Set<String> seenGuestPhones = new HashSet<>();
        List<AddMemberRequest> uniqueRequests = new ArrayList<>();

        for (AddMemberRequest mReq : request.members()) {
            if (mReq == null) continue;

            if (mReq.memberId() != null) {
                if (!seenMemberIds.add(mReq.memberId())) {
                    continue;
                }
            } else {
                validateGuestMember(teamId, mReq.fullName(), mReq.phone());
                if (mReq.phone() != null && !mReq.phone().isBlank()) {
                    String phoneTrimmed = mReq.phone().trim();
                    if (!seenGuestPhones.add(phoneTrimmed)) {
                        throw new BusinessException(
                                ErrorCode.CONFLICT,
                                "Số điện thoại " + phoneTrimmed + " bị trùng lặp trong danh sách yêu cầu"
                        );
                    }
                }
            }
            uniqueRequests.add(mReq);
        }

        Set<UUID> memberIdsToFetch = new HashSet<>();
        for (AddMemberRequest mReq : uniqueRequests) { 
            if (mReq.memberId() != null) {
                if (campaignMemberRepository.existsByCampaignTeam_IdAndMemberId(teamId, mReq.memberId())) {
                    throw new BusinessException(ErrorCode.CONFLICT, "Người dùng đã tồn tại trong đội cứu hộ");
                }
                if (hasActiveAcceptedMembership(mReq.memberId())) {
                    throw new BusinessException(ErrorCode.CONFLICT, "Người dùng hiện đang tham gia một đội cứu hộ khác");
                }
                memberIdsToFetch.add(mReq.memberId());
            }
        }

        Map<UUID, UserInfoGrpcResponse> userMap = memberIdsToFetch.isEmpty()
                ? Map.of()
                : userGrpcClient.getUsersByIds(memberIdsToFetch);

        List<CampaignMember> entitiesToSave = new ArrayList<>();
        for (AddMemberRequest mReq : uniqueRequests) {
            CampaignMember member = campaignMemberMapper.toEntity(mReq);
            member.setCampaignTeam(team);

            if (mReq.memberId() != null) {
                UserInfoGrpcResponse userInfo = userMap.get(mReq.memberId());
                if (userInfo == null) {
                    throw new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "Không tìm thấy thông tin người dùng với ID: " + mReq.memberId()
                    );
                }
                member.setFullName(userInfo.getFullName());
                member.setPhone(userInfo.getPhoneNumber());
                member.setAvatarUrl(userInfo.getAvatarUrl().isBlank() ? null : userInfo.getAvatarUrl());
                member.setSex(SexEnum.from(userInfo.getSex()));
            }

            entitiesToSave.add(member);
        }

        List<CampaignMember> savedMembers = campaignMemberRepository.saveAll(entitiesToSave);
        return savedMembers.stream()
                .map(campaignMemberMapper::toResponse)
                .toList();
    }

    private void validateGuestMember(UUID teamId, String fullName, String phone) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Vui lòng nhập họ tên cho thành viên đi cùng");
        }
        if (phone != null && !phone.isBlank() && campaignMemberRepository.existsByCampaignTeam_IdAndPhone(teamId, phone.trim())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Số điện thoại " + phone.trim() + " đã tồn tại trong đội cứu hộ này");
        }
    }

    @Transactional
    public void removeMember(UUID teamId, UUID memberIdOrRecordId) {
        findTeamById(teamId);

        CampaignMember member = campaignMemberRepository.findById(memberIdOrRecordId)
                .filter(m -> m.getCampaignTeam().getId().equals(teamId))
                .orElseGet(() -> campaignMemberRepository.findByCampaignTeam_IdAndMemberId(teamId, memberIdOrRecordId)
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Không tìm thấy thành viên trong đội cứu hộ này"
                        )));

        if (member.getRoleInTeam() == TeamRole.LEADER) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Không thể xóa Đội trưởng khỏi đội cứu hộ. Để kết thúc hoạt động, vui lòng chọn Off team / Giải tán đội."
            );
        }

        campaignMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getTeamMembers(UUID teamId) {
        findTeamById(teamId);

        return campaignMemberRepository.findByCampaignTeam_Id(teamId)
                .stream()
                .map(campaignMemberMapper::toResponse)
                .toList();
    }

    private CampaignTeam findTeamById(UUID teamId) {
        return campaignTeamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đội cứu hộ với ID: " + teamId
                ));
    }
}
