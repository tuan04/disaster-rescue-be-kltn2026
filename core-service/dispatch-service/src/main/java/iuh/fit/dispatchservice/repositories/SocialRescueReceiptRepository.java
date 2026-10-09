package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.SocialRescueReceipt;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.UUID;


public interface SocialRescueReceiptRepository  extends Repository<SocialRescueReceipt, String> {
    // Đây là INSERT, không phải SELECT.
    @Modifying
    @Transactional(propagation = Propagation.MANDATORY)
    // nativeQuery cho phép dùng cú pháp ON CONFLICT của PostgreSQL.
    @Query(
            value = """
                    INSERT INTO social_rescue_receipts (
                        external_id,
                        approval_event_id,
                        received_at
                    )
                    VALUES (
                        :externalId,
                        :approvalEventId,
                        :receivedAt
                    )
                    ON CONFLICT (external_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int TryRegister(
            @Param("externalId") String externalId,
            @Param("approvalEventId") UUID approvalEventId,
            @Param("receivedAt") Instant receivedAt
    );


    @Modifying
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(
            value = """
                    UPDATE social_rescue_receipts
                    SET rescue_request_id = :rescueRequestId
                    WHERE external_id = :externalId
                      AND approval_event_id = :approvalEventId
                      AND rescue_request_id IS NULL
                    """,
            nativeQuery = true
    )
    int AttachRescueRequest(
            @Param("externalId") String externalId,
            @Param("approvalEventId") UUID approvalEventId,
            @Param("rescueRequestId") UUID rescueRequestId
    );

}
