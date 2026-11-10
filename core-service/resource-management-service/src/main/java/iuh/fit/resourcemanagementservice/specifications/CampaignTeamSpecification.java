package iuh.fit.resourcemanagementservice.specifications;

import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class CampaignTeamSpecification {

    private CampaignTeamSpecification() {}

    public static Specification<CampaignTeam> inActiveCampaign() {
        return (root, query, cb) -> cb.equal(root.join("campaign").get("status"), CampaignStatus.ACTIVE);
    }

    public static Specification<CampaignTeam> hasCampaignId(UUID campaignId) {
        return (root, query, cb) -> campaignId == null ? null : cb.equal(root.get("campaign").get("id"), campaignId);
    }

    public static Specification<CampaignTeam> hasApprovalStatus(TeamApprovalStatus approvalStatus) {
        return (root, query, cb) -> approvalStatus == null ? null : cb.equal(root.get("approvalStatus"), approvalStatus);
    }

    public static Specification<CampaignTeam> hasStatus(TeamStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<CampaignTeam> notDisbanded() {
        return (root, query, cb) -> cb.notEqual(root.get("status"), TeamStatus.DISBANDED);
    }

    public static Specification<CampaignTeam> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("teamName")), pattern),
                    cb.like(cb.lower(root.get("leaderName")), pattern),
                    cb.like(root.get("leaderPhone"), "%" + keyword.trim() + "%"),
                    cb.like(cb.lower(root.get("campaign").get("name")), pattern)
            );
        };
    }
}
