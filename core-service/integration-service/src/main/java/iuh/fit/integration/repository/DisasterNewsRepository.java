package iuh.fit.integration.repository;

import iuh.fit.integration.entity.DisasterNews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisasterNewsRepository extends MongoRepository<DisasterNews, String> {

    boolean existsBySourceUrl(String sourceUrl);

    Optional<DisasterNews> findBySourceUrl(String sourceUrl);

    Page<DisasterNews> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Page<DisasterNews> findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(String keyword, Pageable pageable);
}

