package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
}
