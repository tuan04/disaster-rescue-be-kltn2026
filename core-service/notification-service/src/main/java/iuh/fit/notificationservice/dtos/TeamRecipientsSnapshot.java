package iuh.fit.notificationservice.dtos;

import java.util.List;
import java.util.UUID;

public record TeamRecipientsSnapshot(
        UUID teamId,

        boolean teamExists,

        List<UUID> userIds
) {

    public TeamRecipientsSnapshot {
        userIds = List.copyOf(userIds);
    }
}