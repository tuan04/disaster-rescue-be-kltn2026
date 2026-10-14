package iuh.fit.integration.repository;

import iuh.fit.integration.entity.SocialRawFeed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface SocialRawFeedRepository extends MongoRepository<SocialRawFeed, String> {
    @Query("""
            {
              "status": "PENDING",
              "received_at": {
                "$gte": ?0,
                "$lte": ?1
              },
              "auto_next_check_at": {
                "$lte": ?1
              }
            }
            """)
    List<SocialRawFeed> findDuePendingFeeds(
            Instant enabledSince,
            Instant now,
            Pageable pageable
    );
}
