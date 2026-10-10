package iuh.fit.integration.repository;


import iuh.fit.integration.dtos.request.SocialRawFeedFilterRequest;
import iuh.fit.integration.entity.SocialRawFeed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SocialRawFeedQueryRepository {
    private final MongoTemplate mongoTemplate;

    public Page<SocialRawFeed> search(
            SocialRawFeedFilterRequest request,
            Pageable pageable
    ) {

        Query query = new Query();

        if (request.getSourcePlatform() != null) {
            query.addCriteria(
                    Criteria.where("sourcePlatform")
                            .is(request.getSourcePlatform())
            );
        }

        if (request.getStatus() != null) {
            query.addCriteria(
                    Criteria.where("status")
                            .is(request.getStatus())
            );
        }

        // Đếm tổng số feed phù hợp TRƯỚC khi áp dụng phân trang.
        // Nhờ vậy totalElements không bị giới hạn bằng size.
        long total = mongoTemplate.count(
                query,
                SocialRawFeed.class
        );

        // Áp dụng đồng thời:
        // - số phần tử cần bỏ qua;
        // - số phần tử tối đa;
        // - thứ tự sắp xếp.
        query.with(pageable);


        // Chỉ đọc các feed nằm trong trang đang yêu cầu.
        List<SocialRawFeed> feeds = mongoTemplate.find(
                query,
                SocialRawFeed.class
        );

        // Đóng gói danh sách, thông tin trang và tổng số bản ghi.
        return new PageImpl<>(
                feeds,
                pageable,
                total
        );
    }
}
