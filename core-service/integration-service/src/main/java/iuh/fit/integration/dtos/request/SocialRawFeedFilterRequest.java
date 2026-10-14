package iuh.fit.integration.dtos.request;

import iuh.fit.integration.enums.FeedStatus;
import iuh.fit.integration.enums.SourcePlatform;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialRawFeedFilterRequest {
    private SourcePlatform sourcePlatform;

    // Không truyền thì không lọc trạng thái phê duyệt.
    // Enum hiện tại: PENDING, APPROVED, REJECTED.
    private FeedStatus status;
}
