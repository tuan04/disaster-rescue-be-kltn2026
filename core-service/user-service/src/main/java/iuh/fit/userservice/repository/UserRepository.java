package iuh.fit.userservice.repository;

import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.enums.RoleInTeamEnum;
import iuh.fit.userservice.enums.VerifiedStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
   boolean existsByPhone(String phone);

   User findByPhone(String phone);

   List<User> findByVolunteerProfile_VerifiedStatusAndVolunteerProfile_CurrentRoleInTeam(
           VerifiedStatusEnum status,
           RoleInTeamEnum role
   );

}
