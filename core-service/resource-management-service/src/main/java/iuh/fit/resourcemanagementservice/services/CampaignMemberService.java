package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.request.AddMemberRequest;
import iuh.fit.resourcemanagementservice.dtos.response.MemberResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.repositories.CampaignMemberRepository;
import iuh.fit.resourcemanagementservice.repositories.CampaignTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignMemberService {

    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignTeamRepository campaignTeamRepository;

    /**
     * Thêm thành viên vào Đội Cứu hộ
     * 1. Kiểm tra xem teamId có tồn tại trong campaign_teams hay không.
     * 2. Kiểm tra xem memberId đã tồn tại trong đội này chưa (tránh duplicate).
     * 3. Lưu thành viên mới vào bảng campaign_members.
     * 4. Tự động TĂNG giá trị total_participants của bảng campaign_teams lên 1 và lưu lại.
     */
    @Transactional
    public MemberResponse addMember(UUID teamId, AddMemberRequest request) {
        CampaignTeam team = campaignTeamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đội cứu hộ với ID: " + teamId
                ));

        if (campaignMemberRepository.existsByCampaignTeam_IdAndMemberId(teamId, request.memberId())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Thành viên này đã tồn tại trong đội cứu hộ"
            );
        }

        CampaignMember member = CampaignMember.builder()
                .campaignTeam(team)
                .memberId(request.memberId())
                .roleInTeam(request.roleInTeam())
                .build();

        CampaignMember savedMember = campaignMemberRepository.save(member);

        // Tự động tăng total_participants lên 1
        int currentParticipants = team.getTotalParticipants() != null ? team.getTotalParticipants() : 0;
        team.setTotalParticipants(currentParticipants + 1);
        campaignTeamRepository.save(team);

        return MemberResponse.fromEntity(savedMember);
    }

    /**
     * Xóa thành viên khỏi Đội Cứu hộ
     * 1. Tìm kiếm thành viên trong bảng campaign_members theo teamId và memberId.
     * 2. Xóa bản ghi đó khỏi campaign_members.
     * 3. Tự động GIẢM giá trị total_participants của bảng campaign_teams đi 1 (đảm bảo không âm).
     */
    @Transactional
    public void removeMember(UUID teamId, UUID memberId) {
        CampaignTeam team = campaignTeamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đội cứu hộ với ID: " + teamId
                ));

        CampaignMember member = campaignMemberRepository.findByCampaignTeam_IdAndMemberId(teamId, memberId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy thành viên trong đội cứu hộ này"
                ));

        campaignMemberRepository.delete(member);

        // Tự động giảm total_participants đi 1 (không để âm)
        int currentParticipants = team.getTotalParticipants() != null ? team.getTotalParticipants() : 0;
        team.setTotalParticipants(Math.max(0, currentParticipants - 1));
        campaignTeamRepository.save(team);
    }
}
