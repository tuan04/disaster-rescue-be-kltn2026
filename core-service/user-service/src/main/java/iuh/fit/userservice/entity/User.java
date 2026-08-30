package iuh.fit.userservice.entity;

import iuh.fit.userservice.enums.RoleEnum;
import iuh.fit.userservice.enums.SexEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 20, nullable = false, unique = true)
    private String phone;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private SexEnum sex;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RoleEnum role;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "is_validated", nullable = false)
    private boolean isValidated;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private VolunteerProfile volunteerProfile;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    /**
     * Lấy vai trò hiệu lực của người dùng cho JWT và Response.
     * Nếu role là EMPLOYEE và có VolunteerProfile hợp lệ với currentRoleInTeam, trả về currentRoleInTeam.
     * Ngược lại trả về CITIZEN.
     */
    public String getEffectiveRole() {
        if (this.role == RoleEnum.EMPLOYEE 
                && this.volunteerProfile != null 
                && this.volunteerProfile.getCurrentRoleInTeam() != null) {
            return this.volunteerProfile.getCurrentRoleInTeam().name();
        }
        return RoleEnum.CITIZEN.name();
    }
}
