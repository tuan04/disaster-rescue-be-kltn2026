package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignMemberRepository extends JpaRepository<CampaignMember, UUID> {

    boolean existsByCampaignTeam_IdAndMemberId(UUID campaignTeamId, UUID memberId);

    boolean existsByCampaignTeam_IdAndPhone(UUID campaignTeamId, String phone);

    Optional<CampaignMember> findByCampaignTeam_IdAndMemberId(UUID campaignTeamId, UUID memberId);

    void deleteByCampaignTeam_IdAndMemberId(UUID campaignTeamId, UUID memberId);

    List<CampaignMember> findByCampaignTeam_Id(UUID campaignTeamId);

    @Query("""
        SELECT COUNT(cm) > 0
        FROM CampaignMember cm
        WHERE cm.memberId = :memberId
        AND cm.membershipStatus = iuh.fit.resourcemanagementservice.enums.MembershipStatus.ACCEPTED
        AND cm.campaignTeam.status <> iuh.fit.resourcemanagementservice.enums.TeamStatus.DISBANDED
        AND cm.campaignTeam.approvalStatus <> iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus.REJECTED
    """)
    boolean existsActiveAcceptedMembership(@org.springframework.data.repository.query.Param("memberId") UUID memberId);
}
