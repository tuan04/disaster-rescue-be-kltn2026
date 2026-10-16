package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignTeamRepository extends JpaRepository<CampaignTeam, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT team FROM CampaignTeam team WHERE team.id = :id")
    Optional<CampaignTeam> findByIdForReview(UUID id);

    @Query("""
        SELECT ct
        FROM  CampaignTeam ct JOIN Campaign c
        ON ct.campaign.id = c.id
        WHERE c.status = 'ACTIVE'
        AND ct.leaderId = :leaderId
        AND ct.approvalStatus = 'APPROVED'
        AND ct.status <> 'DISBANDED'
""")
    Optional<CampaignTeam> findActiveTeamByLeaderId(
            UUID leaderId
    );

    @EntityGraph(attributePaths = {"campaign"})
    Optional<CampaignTeam> findFirstByLeaderIdAndStatusNotAndApprovalStatusNot(
            UUID leaderId, TeamStatus status, TeamApprovalStatus approvalStatus);

    boolean existsByIdAndLeaderId(UUID id, UUID leaderId);

    List<CampaignTeam> findByIdInAndStatus(List<UUID> ids, TeamStatus status);

    boolean existsByCampaign_IdAndTeamNameIgnoreCaseAndStatusNotAndApprovalStatusNot(
            UUID campaignId, String teamName, TeamStatus status, TeamApprovalStatus approvalStatus);

    @EntityGraph(attributePaths = {"campaign"})
    @Query("""
        SELECT ct FROM CampaignTeam ct
        WHERE (:campaignId IS NULL OR ct.campaign.id = :campaignId)
          AND (:approvalStatus IS NULL OR ct.approvalStatus = :approvalStatus)
          AND (:status IS NULL OR ct.status = :status)
          AND (:keyword IS NULL OR LOWER(ct.teamName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(ct.leaderName) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<CampaignTeam> findTeamsWithFilters(
            @Param("campaignId") UUID campaignId,
            @Param("approvalStatus") TeamApprovalStatus approvalStatus,
            @Param("status") TeamStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
