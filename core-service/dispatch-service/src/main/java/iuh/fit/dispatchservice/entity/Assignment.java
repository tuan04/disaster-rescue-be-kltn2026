package iuh.fit.dispatchservice.entity;


import iuh.fit.dispatchservice.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Khóa ngoại trỏ về RescueRequest
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private RescueRequest rescueRequest;

    // CẮT REF sang Resource Service
    @Column(name = "campaign_team_id")
    private UUID campaignTeamId;

    @Column(name = "assigned_team_name")
    private String assignedTeamName;

    @Column(name = "leader_phone")
    private String leaderPhone;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
