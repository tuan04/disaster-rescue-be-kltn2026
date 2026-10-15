package iuh.fit.integration.dtos.request;

import iuh.fit.common.social.SocialEvents;

public record FeedInspection(
        // ID feed, cũng là externalId của bài nguồn.
        String feedId,

        boolean eligible,

        String reason,

        SocialEvents.RescueData data
) {
}
