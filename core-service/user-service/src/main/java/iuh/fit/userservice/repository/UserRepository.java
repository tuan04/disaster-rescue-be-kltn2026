package iuh.fit.userservice.repository;

import iuh.fit.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
   boolean existsByPhone(String phone);

   User findByPhone(String phone);

   User findUserByPhoneAndPassword(String phone, String password);
}
