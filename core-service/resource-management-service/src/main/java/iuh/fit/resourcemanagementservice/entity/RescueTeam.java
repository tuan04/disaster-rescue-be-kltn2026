package iuh.fit.resourcemanagementservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rescue_team")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RescueTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "leader_id")
    private UUID leaderId;

    @Column(name = "leader_phone")
    private String leaderPhone;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
