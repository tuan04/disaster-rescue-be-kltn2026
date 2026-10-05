package iuh.fit.userservice.specification;

import iuh.fit.userservice.dto.request.UserFilterRequest;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.entity.VolunteerProfile;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> search(UserFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<User, VolunteerProfile> profile =
                    root.join("volunteerProfile", JoinType.LEFT);

            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = containsPattern(request.getKeyword());

                predicates.add(cb.or(
                        cb.like(
                                cb.lower(root.<String>get("phone")),
                                pattern,
                                '\\'
                        ),
                        cb.like(
                                cb.lower(root.<String>get("fullName")),
                                pattern,
                                '\\'
                        ),
                        cb.like(
                                cb.lower(profile.<String>get("cccdNumber")),
                                pattern,
                                '\\'
                        )
                ));
            }

            if (request.getCurrentRoleInTeam() != null) {
                predicates.add(cb.equal(
                        profile.get("currentRoleInTeam"),
                        request.getCurrentRoleInTeam()
                ));
            }

            if (request.getTeamId() != null) {
                predicates.add(cb.equal(
                        profile.get("teamId"),
                        request.getTeamId()
                ));
            }

            if (request.getVerifiedStatus() != null) {
                predicates.add(cb.equal(
                        profile.get("verifiedStatus"),
                        request.getVerifiedStatus()
                ));
            }

            if (request.getIsValidated() != null) {
                predicates.add(cb.equal(
                        root.<Boolean>get("isValidated"),
                        request.getIsValidated()
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String containsPattern(String keyword) {
        String escaped = keyword.trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        return "%" + escaped + "%";
    }
}