package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.TeamLocationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TeamLocationLogRepository extends JpaRepository<TeamLocationLog, Long> {

    List<TeamLocationLog> findByAssignmentIdOrderByStartedAtAscIdAsc(UUID assignmentId);

    @Query("SELECT COALESCE(SUM(t.distanceMeters), 0.0) FROM TeamLocationLog t WHERE t.assignmentId = :assignmentId")
    Double sumDistanceMetersByAssignmentId(@Param("assignmentId") UUID assignmentId);
}
