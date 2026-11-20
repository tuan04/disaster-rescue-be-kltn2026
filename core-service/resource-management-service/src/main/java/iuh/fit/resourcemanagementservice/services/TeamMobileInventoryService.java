package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.request.CreateTeamMobileInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateTeamMobileInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamMobileInventoryResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.entity.Item;
import iuh.fit.resourcemanagementservice.entity.TeamMobileInventory;
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
    private final CampaignTeamService campaignTeamService;
    private final ItemService itemService;

    /**
     * Bổ sung danh sách vật phẩm vào kho lưu động của đội cứu hộ (hỗ trợ cộng dồn tự động)
     */
    @Transactional
    public List<TeamMobileInventoryResponse> createInventories(CreateTeamMobileInventoryRequest request) {
        CampaignTeam campaignTeam = campaignTeamService.findById(request.campaignTeamId());
        List<TeamMobileInventory> savedList = new java.util.ArrayList<>();

        for (CreateTeamMobileInventoryRequest.TeamItemImport itemReq : request.items()) {
            Optional<TeamMobileInventory> existingOpt = inventoryRepository.findByCampaignTeam_IdAndItem_Id(
                    request.campaignTeamId(),
                    itemReq.itemId()
            );

            TeamMobileInventory inventoryToSave;

            if (existingOpt.isPresent()) {
                TeamMobileInventory existing = existingOpt.get();
                if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                    // Nếu đã từng bị xóa mềm (isDeleted = true), tái kích hoạt lại bản ghi với số lượng mới
                    existing.setCurrentQuantity(itemReq.currentQuantity());
                    existing.setIsDeleted(false);
                } else {
                    // Nếu đã có trong kho lưu động của đội -> Tự động cộng dồn
                    int current = existing.getCurrentQuantity() != null ? existing.getCurrentQuantity() : 0;
                    existing.setCurrentQuantity(current + itemReq.currentQuantity());
                }
                inventoryToSave = existing;
            } else {
                Item item = itemService.findById(itemReq.itemId());
                inventoryToSave = TeamMobileInventory.builder()
                        .campaignTeam(campaignTeam)
                        .item(item)
                        .currentQuantity(itemReq.currentQuantity())
                        .isDeleted(false)
                        .build();
            }

            savedList.add(inventoryRepository.save(inventoryToSave));
        }

        return savedList.stream()
                .map(TeamMobileInventoryResponse::fromEntity)
                .toList();
    }

    /**
     * Cập nhật thông tin tồn kho lưu động hàng loạt (số lượng, trạng thái xóa mềm)
     */
    @Transactional
    public List<TeamMobileInventoryResponse> updateInventories(List<UpdateTeamMobileInventoryRequest> requests) {
        List<TeamMobileInventory> updatedList = new java.util.ArrayList<>();

        for (UpdateTeamMobileInventoryRequest req : requests) {
            TeamMobileInventory existing = inventoryRepository.findByIdAndIsDeletedFalse(req.id())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "Không tìm thấy thông tin tồn kho lưu động với ID: " + req.id()
                    ));

            if (req.currentQuantity() != null) {
                existing.setCurrentQuantity(req.currentQuantity());
            }

            if (req.isDeleted() != null) {
                existing.setIsDeleted(req.isDeleted());
            }

            updatedList.add(inventoryRepository.save(existing));
        }

        return updatedList.stream()
                .map(TeamMobileInventoryResponse::fromEntity)
                .toList();
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

    /**
     * Lấy danh sách toàn bộ vật tư trong kho lưu động của đội cứu hộ
     */
    @Transactional(readOnly = true)
    public List<TeamMobileInventoryResponse> getInventoriesByTeam(UUID teamId) {
        return inventoryRepository.findByCampaignTeam_IdAndIsDeletedFalse(teamId)
                .stream()
                .map(TeamMobileInventoryResponse::fromEntity)
                .toList();
    }

    /**
     * Cộng dồn số lượng vật phẩm vào kho lưu động của đội (sử dụng khi cấp phát từ kho dã chiến)
     */
    @Transactional
    public TeamMobileInventoryResponse addInventoryQuantity(CampaignTeam team, Item item, Integer quantity) {
        Optional<TeamMobileInventory> teamInvOpt = inventoryRepository.findByCampaignTeamIdAndItemIdForUpdate(
                team.getId(),
                item.getId()
        );

        TeamMobileInventory teamInv;
        if (teamInvOpt.isPresent()) {
            teamInv = teamInvOpt.get();
            int current = Boolean.TRUE.equals(teamInv.getIsDeleted()) ? 0 : (teamInv.getCurrentQuantity() != null ? teamInv.getCurrentQuantity() : 0);
            teamInv.setCurrentQuantity(current + quantity);
            teamInv.setIsDeleted(false);
        } else {
            teamInv = TeamMobileInventory.builder()
                    .campaignTeam(team)
                    .item(item)
                    .currentQuantity(quantity)
                    .isDeleted(false)
                    .build();
        }

        TeamMobileInventory savedTeamInv = inventoryRepository.save(teamInv);
        return TeamMobileInventoryResponse.fromEntity(savedTeamInv);
    }
}
