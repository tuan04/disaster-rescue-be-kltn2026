package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.request.CreateCampaignRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateCampaignRequest;
import iuh.fit.resourcemanagementservice.dtos.response.CampaignResponse;
import iuh.fit.resourcemanagementservice.entity.Campaign;
import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import iuh.fit.resourcemanagementservice.repositories.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
                .name(request.getName().trim())
                .startDate(request.getStartDate())
                .endDate(null)
                .status(CampaignStatus.ACTIVE)
                .build();

        Campaign savedCampaign = campaignRepository.save(campaign);

        return CampaignResponse.fromEntity(savedCampaign);
    }

    /**
     * Cập nhật thông tin chiến dịch theo ID
     */
    @Transactional
    public CampaignResponse updateCampaign(UUID id, UpdateCampaignRequest request) {

        Campaign existingCampaign = campaignRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, 
                        "Không tìm thấy chiến dịch với ID: " + id
                ));

        LocalDate effectiveStartDate = request.getStartDate() != null ? request.getStartDate() : existingCampaign.getStartDate();
        LocalDate effectiveEndDate = request.getEndDate() != null ? request.getEndDate() : existingCampaign.getEndDate();

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
        if (request.getEndDate() != null) {
            existingCampaign.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            existingCampaign.setStatus(request.getStatus());
        }

        Campaign updatedCampaign = campaignRepository.save(existingCampaign);

        return CampaignResponse.fromEntity(updatedCampaign);
    }
}
