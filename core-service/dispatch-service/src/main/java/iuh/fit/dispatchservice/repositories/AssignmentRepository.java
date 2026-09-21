package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.Assignment;
import iuh.fit.dispatchservice.enums.AssignmentStatus;
import iuh.fit.dispatchservice.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    Optional<Assignment> findByCampaignTeamIdAndStatus(
            UUID campaignTeamId,
            AssignmentStatus status);

    Optional<Assignment> findByRescueRequestIdAndStatus(
            UUID rescueRequestId,
            AssignmentStatus status);

    List<Assignment> findByRescueRequestIdOrderByCreatedAtDesc(UUID requestId);

    @Query("SELECT a FROM Assignment a WHERE a.campaignTeamId = :campaignTeamId AND a.rescueRequest.id = :rescueRequestId AND a.status = :status" )
    Optional<Assignment> findByCampaignTeamIdAndRescueRequestId(
            @Param("campaignTeamId") UUID campaignTeamId,
            @Param("rescueRequestId") UUID rescueRequestId,
            @Param("status") AssignmentStatus status
    );

    @Query("SELECT a FROM Assignment a WHERE a.rescueRequest.id = :rescueRequestId AND a.status = :status")
    List<Assignment> findAllByRescueRequestIdAndStatus(
            @Param("rescueRequestId") UUID rescueRequestId,
            @Param("status") AssignmentStatus status);

    @Query("SELECT a FROM Assignment a " +
           "JOIN FETCH a.rescueRequest r " +
           "LEFT JOIN FETCH r.mapPoint m " +
           "WHERE a.campaignTeamId = :teamId " +
           "AND a.status = :assignmentStatus " +
           "AND r.status = :requestStatus " +
           "ORDER BY a.assignedAt DESC")
    List<Assignment> findPendingAssignmentsByTeam(
            @Param("teamId") UUID teamId,
            @Param("assignmentStatus") AssignmentStatus assignmentStatus,
            @Param("requestStatus") RequestStatus requestStatus);
}
