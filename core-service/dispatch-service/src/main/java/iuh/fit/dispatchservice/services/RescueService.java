package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.response.PageResponse;
import iuh.fit.dispatchservice.dtos.request.RescueRequestFilterRequest;
import iuh.fit.dispatchservice.dtos.response.RescueRequestListResponse;
import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.repositories.RescueRequestRepository;
import iuh.fit.dispatchservice.repositories.specifications.RescueRequestSearchSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RescueService {
    private final RescueRequestRepository rescueRequestRepository;

    public RescueRequest getRescueRequest(UUID requestId) {
        return rescueRequestRepository.findById(requestId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy yêu cầu cứu hộ"));
    }

    // Đọc dữ liệu và chuyển entity thành DTO trong cùng transaction.
    @Transactional(readOnly = true)
    public PageResponse<RescueRequestListResponse> getRequests(
            RescueRequestFilterRequest request,
            int page, int size
    ) {

        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );
        // page=0 là trang đầu.
        Pageable pageable = PageRequest.of(
                page,
                size,
                sort
        );

        var specification = RescueRequestSearchSpecification.filter(request);

        // Repository thực hiện truy vấn dữ liệu và lấy tổng số phù hợp.
        Page<RescueRequest> result = rescueRequestRepository.findAll(
                specification,
                pageable
        );

        Page<RescueRequestListResponse> responsePage = result.map(
                RescueRequestListResponse::from
        );
        return PageResponse.from(responsePage);
    }


}
