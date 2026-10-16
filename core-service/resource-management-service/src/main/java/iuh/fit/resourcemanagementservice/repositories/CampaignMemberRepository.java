package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignMemberRepository extends JpaRepository<CampaignMember, UUID> {

    Optional<CampaignMember> findByCampaignTeam_IdAndMemberId(UUID campaignTeamId, UUID memberId);

    List<CampaignMember> findByCampaignTeam_Id(UUID campaignTeamId);

    List<CampaignMember> findByCampaignTeam_IdAndMemberIdIn(UUID campaignTeamId, Collection<UUID> memberIds);

    @Query("""
        SELECT cm.phone
        FROM CampaignMember cm
        WHERE cm.campaignTeam.id = :campaignTeamId
        AND cm.phone IN :phones
        AND cm.membershipStatus <> iuh.fit.resourcemanagementservice.enums.MembershipStatus.CANCELLED
    """)
    List<String> findExistingPhonesInTeam(
            @Param("campaignTeamId") UUID campaignTeamId,
            @Param("phones") Collection<String> phones
    );

    @Query("""
        SELECT cm.memberId
        FROM CampaignMember cm
        WHERE cm.memberId IN :memberIds
        AND cm.membershipStatus = iuh.fit.resourcemanagementservice.enums.MembershipStatus.ACCEPTED
        AND cm.campaignTeam.status <> iuh.fit.resourcemanagementservice.enums.TeamStatus.DISBANDED
        AND cm.campaignTeam.approvalStatus <> iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus.REJECTED
        AND cm.campaignTeam.campaign.status = iuh.fit.resourcemanagementservice.enums.CampaignStatus.ACTIVE
    """)
    List<UUID> findActiveAcceptedMemberIds(@Param("memberIds") Collection<UUID> memberIds);

    @Query("""
        SELECT COUNT(cm) > 0
        FROM CampaignMember cm
        WHERE cm.memberId = :memberId
        AND cm.membershipStatus = iuh.fit.resourcemanagementservice.enums.MembershipStatus.ACCEPTED
        AND cm.campaignTeam.status <> iuh.fit.resourcemanagementservice.enums.TeamStatus.DISBANDED
        AND cm.campaignTeam.approvalStatus <> iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus.REJECTED
        AND cm.campaignTeam.campaign.status = iuh.fit.resourcemanagementservice.enums.CampaignStatus.ACTIVE
    """)
    boolean existsActiveAcceptedMembership(@Param("memberId") UUID memberId);

    @Query("""
        SELECT cm FROM CampaignMember cm
        JOIN FETCH cm.campaignTeam ct
        JOIN FETCH ct.campaign c
        WHERE cm.memberId = :memberId
        AND cm.membershipStatus = iuh.fit.resourcemanagementservice.enums.MembershipStatus.INVITED
        AND ct.status <> iuh.fit.resourcemanagementservice.enums.TeamStatus.DISBANDED
        AND ct.approvalStatus = iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus.APPROVED
        AND c.status = iuh.fit.resourcemanagementservice.enums.CampaignStatus.ACTIVE
        ORDER BY cm.createdAt DESC
    """)
    List<CampaignMember> findMyInvitations(@Param("memberId") UUID memberId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE CampaignMember cm
        SET cm.membershipStatus = iuh.fit.resourcemanagementservice.enums.MembershipStatus.CANCELLED,
            cm.modifiedAt = CURRENT_TIMESTAMP
        WHERE cm.memberId = :memberId
        AND cm.id <> :acceptedMembershipId
        AND cm.membershipStatus = iuh.fit.resourcemanagementservice.enums.MembershipStatus.INVITED
    """)
    int cancelOtherPendingInvitations(
            @Param("memberId") UUID memberId,
            @Param("acceptedMembershipId") UUID acceptedMembershipId
    );

    Optional<CampaignMember> findByIdAndCampaignTeam_Id(UUID id, UUID campaignTeamId);

    @Query("SELECT cm FROM CampaignMember cm WHERE cm.campaignTeam.id = :teamId AND cm.membershipStatus IN ('ACCEPTED', 'INVITED')")
    List<CampaignMember> findActiveMembersByTeamId(@Param("teamId") UUID teamId);

    @Query("""
        SELECT cm FROM CampaignMember cm
        JOIN FETCH cm.campaignTeam ct
        JOIN FETCH ct.campaign c
        WHERE cm.id = :id
    """)
    Optional<CampaignMember> findByIdWithTeamAndCampaign(@Param("id") UUID id);

    @Query("""
        SELECT ct FROM CampaignMember cm
        JOIN cm.campaignTeam ct
        JOIN FETCH ct.campaign c
        WHERE cm.memberId = :memberId
          AND cm.membershipStatus = iuh.fit.resourcemanagementservice.enums.MembershipStatus.ACCEPTED
          AND ct.status <> iuh.fit.resourcemanagementservice.enums.TeamStatus.DISBANDED
          AND ct.approvalStatus = iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus.APPROVED
          AND c.status = iuh.fit.resourcemanagementservice.enums.CampaignStatus.ACTIVE
    """)
    Optional<CampaignTeam> findActiveTeamByMemberId(@Param("memberId") UUID memberId);
}
