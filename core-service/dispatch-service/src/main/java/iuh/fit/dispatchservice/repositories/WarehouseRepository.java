package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
}
