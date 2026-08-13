package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.HazardReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HazardReportRepository extends JpaRepository<HazardReport, UUID> {
}
