package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.Assignment;
import iuh.fit.dispatchservice.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    Optional<Assignment> findByCampaignTeamIdAndStatus(
            UUID campaignTeamId,
            AssignmentStatus status);
}
