package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignTeamRepository extends JpaRepository<CampaignTeam, UUID>, JpaSpecificationExecutor<CampaignTeam> {
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

    @Override
    @EntityGraph(attributePaths = {"campaign"})
    Page<CampaignTeam> findAll(Specification<CampaignTeam> spec, Pageable pageable);
}
