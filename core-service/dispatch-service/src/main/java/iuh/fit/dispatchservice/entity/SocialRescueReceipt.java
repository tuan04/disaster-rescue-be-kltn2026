package iuh.fit.dispatchservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "social_rescue_receipts")
@Getter
@NoArgsConstructor
public class SocialRescueReceipt {
    @Id
    @Column(name = "external_id", nullable = false, columnDefinition = "TEXT")
    private String externalId;


    @Column(name = "approval_event_id", nullable = false)
    private UUID approvalEventId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "rescue_request_id",
            unique = true,
            nullable = true
    )
    private RescueRequest rescueRequest;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;
}
