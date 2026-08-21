package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.repositories.RescueRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
