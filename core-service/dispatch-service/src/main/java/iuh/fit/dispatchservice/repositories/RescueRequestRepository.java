package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.RescueRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RescueRequestRepository extends JpaRepository<RescueRequest, UUID> {

}
