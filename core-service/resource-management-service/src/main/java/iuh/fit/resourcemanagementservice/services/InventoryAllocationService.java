package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.request.AllocateInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamMobileInventoryResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.entity.CampaignWarehouseInventory;
import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryAllocationService {

    private final CampaignWarehouseInventoryService warehouseInventoryService;
    private final TeamMobileInventoryService teamMobileInventoryService;
    private final CampaignTeamService campaignTeamService;

    /**
     * Điều phối cấp phát vật tư từ Kho cứu trợ dã chiến sang Đội cứu hộ
     */
    @Transactional
    public List<TeamMobileInventoryResponse> allocateInventory(AllocateInventoryRequest request) {
        // 1. Kiểm tra tính hợp lệ của Đội cứu hộ nhận cấp phát
        CampaignTeam team = campaignTeamService.findById(request.campaignTeamId());
        if (team.getApprovalStatus() != TeamApprovalStatus.APPROVED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Đội cứu hộ \"" + team.getTeamName() + "\" chưa được phê duyệt"
            );
        }
        if (team.getStatus() == TeamStatus.DISBANDED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Đội cứu hộ \"" + team.getTeamName() + "\" đã giải tán"
            );
        }
        if (team.getCampaign() != null && team.getCampaign().getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Chiến dịch \"" + team.getCampaign().getName() + "\" của đội cứu hộ đã kết thúc hoặc không còn hoạt động"
            );
        }

        // 2. Kiểm tra danh sách vật phẩm cấp phát không được rỗng
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Danh sách vật phẩm cấp phát không được để trống");
        }

        // 3. Kiểm tra tính toàn vẹn: Không cho phép trùng lặp cùng 1 mặt hàng trong 1 phiếu cấp phát
        Set<UUID> seenItems = new HashSet<>();
        for (AllocateInventoryRequest.ItemAllocation itemReq : request.items()) {
            if (!seenItems.add(itemReq.itemId())) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "Danh sách cấp phát có vật phẩm bị trùng lặp trong yêu cầu (itemId: " + itemReq.itemId() + ")"
                );
            }
        }

        // 4. Trừ tồn kho tại kho cứu trợ và cộng dồn vào kho lưu động của đội cứu hộ cho từng mặt hàng
        List<TeamMobileInventoryResponse> results = new ArrayList<>();
        for (AllocateInventoryRequest.ItemAllocation itemReq : request.items()) {
            // Trừ số lượng tồn kho (sử dụng Pessimistic Lock và validate từng món)
            CampaignWarehouseInventory warehouseInv = warehouseInventoryService.deductInventoryQuantity(request.warehouseId(), itemReq);

            // Cộng dồn số lượng vào kho lưu động của đội cứu hộ
            TeamMobileInventoryResponse teamInv = teamMobileInventoryService.addInventoryQuantity(
                    team,
                    warehouseInv.getItem(),
                    itemReq.quantity()
            );
            results.add(teamInv);
        }

        return results;
    }
}
