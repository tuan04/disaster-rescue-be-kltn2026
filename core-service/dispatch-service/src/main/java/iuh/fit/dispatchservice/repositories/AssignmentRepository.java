package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.dtos.projection.TeamRescueStatisticsProjection;
import iuh.fit.dispatchservice.dtos.response.OngoingAssignmentResponse;
import iuh.fit.dispatchservice.entity.Assignment;
import iuh.fit.dispatchservice.enums.AssignmentStatus;
import iuh.fit.dispatchservice.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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




    @Query(
            value = """
                select new iuh.fit.dispatchservice.dtos.response.OngoingAssignmentResponse(
                    a.id,
                    r.id,
                    r.code,
                    a.campaignTeamId,
                    a.assignedTeamName,
                    a.status,
                    coalesce(
                        a.modifiedAt,
                        a.respondedAt,
                        a.assignedAt,
                        a.createdAt
                    )
                )
                from Assignment a
                join a.rescueRequest r
                where a.status in :assignmentStatuses
                  and r.status in :requestStatuses
                order by
                    coalesce(
                        a.modifiedAt,
                        a.respondedAt,
                        a.assignedAt,
                        a.createdAt
                    ) desc,
                    a.id desc
                """,
            countQuery = """
                select count(a)
                from Assignment a
                join a.rescueRequest r
                where a.status in :assignmentStatuses
                  and r.status in :requestStatuses
                """
    )
    Page<OngoingAssignmentResponse> findOngoingAssignments(
            @Param("assignmentStatuses")
            List<AssignmentStatus> assignmentStatuses,

            @Param("requestStatuses")
            List<RequestStatus> requestStatuses,

            Pageable pageable
    );

    @EntityGraph(attributePaths = {"rescueRequest", "rescueRequest.mapPoint"})
    @Query("SELECT a FROM Assignment a " +
            "WHERE a.campaignTeamId = :teamId AND a.status IN :statuses " +
            "ORDER BY COALESCE(a.completedAt, a.modifiedAt, a.respondedAt, a.assignedAt, a.createdAt) DESC, a.id DESC")
    Page<Assignment> findTeamHistoryByStatuses(
            @Param("teamId") UUID teamId,
            @Param("statuses") List<AssignmentStatus> statuses,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"rescueRequest", "rescueRequest.mapPoint"})
    Optional<Assignment> findWithRequestAndMapPointById(UUID id);

    @Query(value = """
            SELECT
                COALESCE(SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS totalCompletedCases,
                COALESCE(SUM(CASE WHEN a.status = 'CANCELED' THEN 1 ELSE 0 END), 0) AS totalCanceledCases,
                COALESCE(SUM(CASE WHEN a.status = 'REJECTED' THEN 1 ELSE 0 END), 0) AS totalRejectedCases,
                COALESCE(SUM(CASE WHEN a.status IN ('COMPLETED', 'CANCELED') THEN COALESCE(a.distance_meters, 0) ELSE 0 END), 0) AS totalDistanceMeters,
                AVG(CASE
                    WHEN a.status = 'COMPLETED'
                         AND a.completed_at IS NOT NULL
                         AND COALESCE(a.responded_at, a.assigned_at) IS NOT NULL
                    THEN GREATEST(0, EXTRACT(EPOCH FROM (a.completed_at - COALESCE(a.responded_at, a.assigned_at))) / 60.0)
                    ELSE NULL
                END) AS averageDurationMinutes
            FROM assignments a
            WHERE a.campaign_team_id = :teamId
            """, nativeQuery = true)
    TeamRescueStatisticsProjection aggregateTeamStatistics(@Param("teamId") UUID teamId);
}
