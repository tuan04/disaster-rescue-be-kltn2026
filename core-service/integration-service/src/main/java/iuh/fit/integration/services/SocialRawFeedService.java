package iuh.fit.integration.services;


import iuh.fit.common.response.PageResponse;
import iuh.fit.integration.dtos.request.SocialRawFeedFilterRequest;
import iuh.fit.integration.entity.SocialRawFeed;
import iuh.fit.integration.repository.SocialRawFeedQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialRawFeedService {
    private final SocialRawFeedQueryRepository queryRepository;

    public PageResponse<SocialRawFeed> searchFeeds(
            SocialRawFeedFilterRequest request,
            Pageable pageable
    ) {
        Sort sort = Sort.by(
                Sort.Order.desc("receivedAt"),
                Sort.Order.desc("id")
        );
        Pageable pageableNew = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
        var pageData = queryRepository.search(request, pageableNew);
        return PageResponse.from(pageData);
    }
}
