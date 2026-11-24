package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.dtos.projection.RescueRequestStatusProjection;
import iuh.fit.dispatchservice.dtos.projection.RescueStatisticsProjection;
import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.enums.AssignmentStatus;
import iuh.fit.dispatchservice.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RescueRequestRepository extends JpaRepository<RescueRequest, UUID>, JpaSpecificationExecutor<RescueRequest> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"mapPoint"})
    Page<RescueRequest> findAll(Specification<RescueRequest> spec, @NonNull Pageable pageable);

    @Query("SELECT r.id AS id, r.status AS status FROM RescueRequest r WHERE r.id IN :ids")
    List<RescueRequestStatusProjection> findStatusesByIdIn(@Param("ids") Collection<UUID> ids);

    @EntityGraph(attributePaths = {"mapPoint"})
    List<RescueRequest> findByReporterIdOrderByCreatedAtDescIdDesc(UUID reporterId);

    // 1. Chờ tiếp nhận: PENDING và không có phân công đang hoạt động.
    @Query("""
        select count(r)
        from RescueRequest r
        where r.status = :pendingStatus
          and not exists (
              select a.id
              from Assignment a
              where a.rescueRequest = r
                and a.status in :activeStatuses
          )
        """)
    long countWaitingRequests(
            @Param("pendingStatus") RequestStatus pendingStatus,
            @Param("activeStatuses")
            Collection<AssignmentStatus> activeStatuses
    );


    // 2. Đang xử lý: đếm yêu cầu, không đếm số đội được phân công.
    @Query("""
        select count(r)
        from RescueRequest r
        where r.status in :requestStatuses
          and exists (
              select a.id
              from Assignment a
              where a.rescueRequest = r
                and a.status in :activeStatuses
          )
        """)
    long countProcessingRequests(
            @Param("requestStatuses")
            Collection<RequestStatus> requestStatuses,

            @Param("activeStatuses")
            Collection<AssignmentStatus> activeStatuses
    );



    // 3. Gửi trong ngày được chọn và hiện đã COMPLETED.
   // Khoảng thời gian: [đầu ngày, đầu ngày tiếp theo).
    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            RequestStatus status,
            LocalDateTime from,
            LocalDateTime to
    );





    @Query(value = """
        SELECT
            
            CAST(
                date_trunc(:periodUnit, r.created_at)
                AS date
            ) AS "periodStart",

        
            -- Đã cứu hộ trước khi ngày gửi kết thúc.
              COUNT(*) FILTER (
                  WHERE r.complete_at <
                        date_trunc('day', r.created_at) + INTERVAL '1 day'
              ) AS "completed",
              
              -- Báo an toàn trong ngày gửi, chưa được tính là đã cứu hộ.
              COUNT(*) FILTER (
                  WHERE r.safe_at <
                        date_trunc('day', r.created_at) + INTERVAL '1 day'
                    AND (
                        r.complete_at IS NULL
                        OR r.complete_at >=
                           date_trunc('day', r.created_at) + INTERVAL '1 day'
                    )
              ) AS "safe",
              
              -- Đến hết ngày gửi vẫn chưa hoàn thành và chưa báo an toàn.
              COUNT(*) FILTER (
                  WHERE (
                      r.complete_at IS NULL
                      OR r.complete_at >=
                         date_trunc('day', r.created_at) + INTERVAL '1 day'
                  )
                  AND (
                      r.safe_at IS NULL
                      OR r.safe_at >=
                         date_trunc('day', r.created_at) + INTERVAL '1 day'
                  )
              ) AS "pending"

        

        FROM rescue_requests r

        WHERE r.created_at >= :fromTime
          AND r.created_at < :toTime
          AND r.status IN ('COMPLETED', 'SAFE', 'PENDING')

        GROUP BY 1
        ORDER BY 1
        """, nativeQuery = true)
    List<RescueStatisticsProjection> aggregateRescueStatistics(
            @Param("periodUnit") String periodUnit,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );
}

