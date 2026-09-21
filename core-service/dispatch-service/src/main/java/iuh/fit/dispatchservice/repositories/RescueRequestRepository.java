package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.RescueRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;

import java.util.UUID;

public interface RescueRequestRepository extends JpaRepository<RescueRequest, UUID>, JpaSpecificationExecutor<RescueRequest> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"mapPoint"})
    Page<RescueRequest> findAll(Specification<RescueRequest> spec, @NonNull Pageable pageable);
}

