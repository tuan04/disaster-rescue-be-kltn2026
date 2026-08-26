package iuh.fit.notificationservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeamLocationLogRepository extends JpaRepository<TeamLocationLogRepository, UUID> {
}
