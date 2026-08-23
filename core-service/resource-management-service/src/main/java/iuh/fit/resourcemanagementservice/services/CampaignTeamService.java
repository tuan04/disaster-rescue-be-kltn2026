package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.request.CreateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import iuh.fit.resourcemanagementservice.repositories.CampaignTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignTeamService {

    private final CampaignTeamRepository campaignTeamRepository;

    /**
     * Tạo mới Đội Cứu hộ (Campaign Team)
     * Business Logic:
     * - totalParticipants = 0
     * - vehicles = []
     * - status = OFFLINE
     * - createdAt = LocalDateTime.now()
     */
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {

        CampaignTeam team = CampaignTeam.builder()
                .campaignId(request.campaignId())
                .teamName(request.teamName() != null ? request.teamName().trim() : null)
                .leaderId(request.leaderId())
                .leaderPhone(request.leaderPhone().trim())
                .totalParticipants(0)
                .vehicles(new ArrayList<>())
                .status(TeamStatus.OFFLINE)
                .createdAt(LocalDateTime.now())
                .build();

        CampaignTeam savedTeam = campaignTeamRepository.save(team);

        return TeamResponse.fromEntity(savedTeam);
    }

    /**
     * Cập nhật thông tin Đội Cứu hộ theo ID
     */
    @Transactional
    public TeamResponse updateTeam(UUID id, UpdateTeamRequest request) {

        CampaignTeam existingTeam = campaignTeamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đội cứu hộ với ID: " + id
                ));

        existingTeam.setCampaignId(request.campaignId());
        existingTeam.setTotalParticipants(request.totalParticipants() != null ? request.totalParticipants() : 0);
        existingTeam.setStatus(request.status());
        existingTeam.setVehicles(request.vehicles() != null ? new ArrayList<>(request.vehicles()) : new ArrayList<>());
        existingTeam.setTeamName(request.teamName() != null ? request.teamName().trim() : null);
        existingTeam.setLeaderId(request.leaderId());
        existingTeam.setLeaderPhone(request.leaderPhone().trim());

        CampaignTeam updatedTeam = campaignTeamRepository.save(existingTeam);
        return TeamResponse.fromEntity(updatedTeam);
    }


    /**
     * Truy vấn thông tin đội cứu hộ theo Leader ID (phục vụ gRPC/nội bộ)
     */
    @Transactional(readOnly = true)
    public CampaignTeam getTeamByLeaderId(UUID leaderId) {
        return campaignTeamRepository.findByLeaderId(leaderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đội cứu hộ cho Leader ID: " + leaderId));
    }
}
