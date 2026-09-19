package iuh.fit.dispatchservice.repositories.specifications;

import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.enums.EmergencyLevel;
import iuh.fit.dispatchservice.enums.RequestSource;
import iuh.fit.dispatchservice.enums.RequestStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RescueRequestSpecification {

    public static Specification<RescueRequest> filter(
            RequestStatus status,
            EmergencyLevel emergencyLevel,
            RequestSource source
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (emergencyLevel != null) {
                predicates.add(criteriaBuilder.equal(root.get("emergencyLevel"), emergencyLevel));
            }

            if (source != null) {
                predicates.add(criteriaBuilder.equal(root.get("source"), source));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
