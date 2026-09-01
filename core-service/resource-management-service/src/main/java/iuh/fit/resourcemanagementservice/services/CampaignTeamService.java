package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.redis.TeamLocation;
import iuh.fit.resourcemanagementservice.dtos.request.TeamLocationRequest;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.repositories.CampaignTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import iuh.fit.resourcemanagementservice.dtos.request.CreateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamResponse;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignTeamService {

    private final CampaignTeamRepository campaignTeamRepository;
    private final RedisService redisService;

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
     * Cập nhật thông tin Đội Cứu hộ theo ID (Cập nhật linh hoạt - Partial Update)
     * Chỉ cập nhật các trường được truyền lên (khác null).
     */
    @Transactional
    public TeamResponse updateTeam(UUID id, UpdateTeamRequest request) {

        CampaignTeam existingTeam = campaignTeamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đội cứu hộ với ID: " + id));

        if (request.totalParticipants() != null) {
            existingTeam.setTotalParticipants(request.totalParticipants());
        }

        if (request.status() != null) {
            existingTeam.setStatus(request.status());
        }

        if (request.vehicles() != null) {
            existingTeam.setVehicles(new ArrayList<>(request.vehicles()));
        }

        if (request.teamName() != null) {
            existingTeam.setTeamName(request.teamName().trim());
        }

        if (request.leaderId() != null) {
            existingTeam.setLeaderId(request.leaderId());
        }

        if (request.leaderPhone() != null && !request.leaderPhone().isBlank()) {
            existingTeam.setLeaderPhone(request.leaderPhone().trim());
        }

        CampaignTeam updatedTeam = campaignTeamRepository.save(existingTeam);
        return TeamResponse.fromEntity(updatedTeam);
    }

    /**
     * Truy vấn thông tin đội cứu hộ trong chiến dịch hiện tại theo Leader ID (phục vụ gRPC/nội bộ)
     */
    public CampaignTeam getActiveTeamByLeaderId(UUID leaderId) {
        return campaignTeamRepository.findActiveTeamByLeaderId(leaderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy đội cứu hộ cho Leader ID: " + leaderId));
    }

    public void saveTeamLocation(UUID leaderId, TeamLocationRequest request) {
        CampaignTeam team = this.getActiveTeamByLeaderId(leaderId);

        TeamLocation teamLocation = TeamLocation.builder()
                .latitude(request.latitude())
                .longitude(request.longitude())
                .speed(request.speed())
                .heading(request.heading())
                .recordedAt(Instant.now())
                .build();

        redisService.saveLocation(team.getId(), teamLocation);
    }
}
