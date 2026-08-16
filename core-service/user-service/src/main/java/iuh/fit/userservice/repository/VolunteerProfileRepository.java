package iuh.fit.userservice.repository;

import iuh.fit.userservice.entity.VolunteerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VolunteerProfileRepository extends JpaRepository<VolunteerProfile, UUID> {
}
