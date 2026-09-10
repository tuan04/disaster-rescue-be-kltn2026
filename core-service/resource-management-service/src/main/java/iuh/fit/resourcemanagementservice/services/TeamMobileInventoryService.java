package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.request.CreateTeamMobileInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateTeamMobileInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamMobileInventoryResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.entity.Item;
import iuh.fit.resourcemanagementservice.entity.TeamMobileInventory;
import iuh.fit.resourcemanagementservice.repositories.CampaignTeamRepository;
import iuh.fit.resourcemanagementservice.repositories.ItemRepository;
import iuh.fit.resourcemanagementservice.repositories.TeamMobileInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamMobileInventoryService {

    private final TeamMobileInventoryRepository inventoryRepository;
    private final CampaignTeamRepository campaignTeamRepository;
    private final ItemRepository itemRepository;

    /**
     * Thêm mới vật phẩm vào kho lưu động của đội cứu hộ
     */
    @Transactional
    public TeamMobileInventoryResponse createInventory(CreateTeamMobileInventoryRequest request) {
        CampaignTeam campaignTeam = campaignTeamRepository.findById(request.campaignTeamId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đội cứu hộ với ID: " + request.campaignTeamId()
                ));

        Item item = itemRepository.findByIdAndIsDeletedFalse(request.itemId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy vật phẩm hoặc vật phẩm đã bị xóa với ID: " + request.itemId()
                ));

        Optional<TeamMobileInventory> existingOpt = inventoryRepository.findByCampaignTeam_IdAndItem_Id(
                request.campaignTeamId(),
                request.itemId()
        );

        TeamMobileInventory inventoryToSave;

        if (existingOpt.isPresent()) {
            TeamMobileInventory existing = existingOpt.get();
            if (Boolean.FALSE.equals(existing.getIsDeleted())) {
                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "Vật phẩm này đã tồn tại trong kho lưu động của đội cứu hộ"
                );
            }
            // Nếu đã từng bị xóa mềm (isDeleted = true), tái kích hoạt lại bản ghi
            existing.setCurrentQuantity(request.currentQuantity());
            existing.setIsDeleted(false);
            inventoryToSave = existing;
        } else {
            inventoryToSave = TeamMobileInventory.builder()
                    .campaignTeam(campaignTeam)
                    .item(item)
                    .currentQuantity(request.currentQuantity())
                    .isDeleted(false)
                    .build();
        }

        TeamMobileInventory saved = inventoryRepository.save(inventoryToSave);
        return TeamMobileInventoryResponse.fromEntity(saved);
    }

    /**
     * Cập nhật thông tin tồn kho lưu động (số lượng, trạng thái xóa mềm)
     */
    @Transactional
    public TeamMobileInventoryResponse updateInventory(UUID id, UpdateTeamMobileInventoryRequest request) {
        TeamMobileInventory existing = inventoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy thông tin tồn kho lưu động với ID: " + id
                ));

        if (request.currentQuantity() != null) {
            existing.setCurrentQuantity(request.currentQuantity());
        }

        if (request.isDeleted() != null) {
            existing.setIsDeleted(request.isDeleted());
        }

        TeamMobileInventory updated = inventoryRepository.save(existing);
        return TeamMobileInventoryResponse.fromEntity(updated);
    }

    /**
     * Xóa mềm tồn kho lưu động (cập nhật isDeleted = true)
     */
    @Transactional
    public void deleteInventory(UUID id) {
        TeamMobileInventory existing = inventoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy thông tin tồn kho lưu động với ID: " + id
                ));

        existing.setIsDeleted(true);
        inventoryRepository.save(existing);
    }
}
