package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.Assignment;
import iuh.fit.dispatchservice.enums.AssignmentStatus;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
