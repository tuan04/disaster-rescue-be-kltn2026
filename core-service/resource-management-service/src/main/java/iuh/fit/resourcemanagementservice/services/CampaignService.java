package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.request.CreateCampaignRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateCampaignRequest;
import iuh.fit.resourcemanagementservice.dtos.response.CampaignResponse;
import iuh.fit.resourcemanagementservice.entity.Campaign;
import iuh.fit.resourcemanagementservice.utils.CampaignCode;
import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import iuh.fit.resourcemanagementservice.repositories.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.common.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;

    /**
     * Tạo mới chiến dịch (Campaign)
     * Mặc định: status = ACTIVE, endDate = null
     */
    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        Campaign campaign = Campaign.builder()
                .code(CampaignCode.generate(request.getName(), campaignRepository.nextCodeSequence()))
                .name(request.getName().trim())
                .startDate(request.getStartDate())
                .endDate(null)
                .status(CampaignStatus.ACTIVE)
                .build();

        Campaign savedCampaign = campaignRepository.save(campaign);

        return CampaignResponse.fromEntity(savedCampaign);
    }

    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> getCampaigns(String keyword, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Trang phải từ 0 và kích thước trang từ 1 đến 100");
        }
        var pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var result = keyword == null || keyword.isBlank()
                ? campaignRepository.findAll(pageable)
                : campaignRepository.searchByNameOrCode(keyword.strip(), pageable);
        return PageResponse.from(result.map(CampaignResponse::fromEntity));
    }

    public Campaign findById(UUID id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chiến dịch cứu hộ"
                ));
    }

    /**
     * Cập nhật thông tin chiến dịch theo ID
     */
    @Transactional
    public CampaignResponse updateCampaign(UUID id, UpdateCampaignRequest request) {

        Campaign existingCampaign = findById(id);

        if (request.getName() != null && request.getName().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tên chiến dịch không được để trống");
        }
        boolean clearEndDate = Boolean.TRUE.equals(request.getClearEndDate());
        if (clearEndDate && request.getEndDate() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Không thể vừa xóa vừa đặt ngày kết thúc");
        }
        LocalDate effectiveStartDate = request.getStartDate() != null ? request.getStartDate() : existingCampaign.getStartDate();
        LocalDate effectiveEndDate = clearEndDate ? null
                : request.getEndDate() != null ? request.getEndDate() : existingCampaign.getEndDate();

        if (effectiveEndDate != null && effectiveStartDate != null && effectiveEndDate.isBefore(effectiveStartDate)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT, 
                    "Ngày kết thúc không được trước ngày bắt đầu"
            );
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            existingCampaign.setName(request.getName().trim());
        }
        if (request.getStartDate() != null) {
            existingCampaign.setStartDate(request.getStartDate());
        }
        if (clearEndDate || request.getEndDate() != null) {
            existingCampaign.setEndDate(effectiveEndDate);
        }
        if (request.getStatus() != null) {
            existingCampaign.setStatus(request.getStatus());
        }

        Campaign updatedCampaign = campaignRepository.save(existingCampaign);

        return CampaignResponse.fromEntity(updatedCampaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getActiveCampaigns() {
        return campaignRepository.findByStatus(CampaignStatus.ACTIVE)
                .stream()
                .map(CampaignResponse::fromEntity)
                .toList();
    }
}
