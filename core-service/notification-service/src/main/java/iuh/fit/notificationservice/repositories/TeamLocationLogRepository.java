package iuh.fit.notificationservice.repositories;

import iuh.fit.notificationservice.entity.TeamLocationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeamLocationLogRepository extends JpaRepository<TeamLocationLog, UUID> {
}
