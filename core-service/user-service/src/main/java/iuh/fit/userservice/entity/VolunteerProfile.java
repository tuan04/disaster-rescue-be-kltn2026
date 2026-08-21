package iuh.fit.userservice.entity;

import iuh.fit.userservice.enums.VerifiedStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "volunteer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cccd_number", length = 12)
    private String cccdNumber;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "team_id")
    private UUID teamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_status", length = 20)
    private VerifiedStatusEnum verifiedStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;
}