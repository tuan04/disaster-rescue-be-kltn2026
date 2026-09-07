package iuh.fit.notificationservice.repositories;

import iuh.fit.notificationservice.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    @Query(value = "SELECT un FROM UserNotification un JOIN FETCH un.notification n " +
           "WHERE un.userId = :userId AND un.isDeleted = false " +
           "ORDER BY un.createdAt DESC",
           countQuery = "SELECT count(un) FROM UserNotification un " +
           "WHERE un.userId = :userId AND un.isDeleted = false")
    Page<UserNotification> findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT un FROM UserNotification un JOIN FETCH un.notification n " +
           "WHERE un.userId = :userId AND un.isDeleted = false " +
           "ORDER BY un.createdAt DESC")
    List<UserNotification> findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("userId") UUID userId);

    Optional<UserNotification> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE UserNotification un SET un.isRead = true, un.modifiedAt = CURRENT_TIMESTAMP " +
           "WHERE un.userId = :userId AND un.isDeleted = false AND un.isRead = false")
    int markAllAsReadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE UserNotification un SET un.isDeleted = true, un.modifiedAt = CURRENT_TIMESTAMP " +
           "WHERE un.userId = :userId AND un.isDeleted = false")
    int markAllAsDeletedByUserId(@Param("userId") UUID userId);
}
