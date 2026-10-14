package iuh.fit.resourcemanagementservice.dtos.response;

import java.util.List;
import java.util.UUID;

public record TeamRecipientsResult(
        UUID teamId,
        boolean teamExists,
        List<UUID> userIds
) {
    public TeamRecipientsResult {
        userIds = List.copyOf(userIds);
    }
}
