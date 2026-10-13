package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.kafka.dto.TeamLocationUpdatedEvent;
import iuh.fit.resourcemanagementservice.dtos.redis.TeamLocation;
import iuh.fit.resourcemanagementservice.dtos.request.RejectTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.request.TeamLocationRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamResponse;
import iuh.fit.resourcemanagementservice.entity.Campaign;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus;
import iuh.fit.resourcemanagementservice.events.producer.LocationEventProducer;
import iuh.fit.resourcemanagementservice.repositories.CampaignTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import iuh.fit.common.grpc.UserInfoGrpcResponse;
import iuh.fit.resourcemanagementservice.client.UserGrpcClient;
import iuh.fit.resourcemanagementservice.mappers.CampaignTeamMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignTeamService {

    private final CampaignTeamRepository campaignTeamRepository;
    private final CampaignService campaignService;
    private final CampaignMemberService campaignMemberService;
    private final CampaignTeamMapper campaignTeamMapper;
    private final UserGrpcClient userGrpcClient;
    private final RedisService redisService;
    private final LocationEventProducer locationEventProducer;

    public CampaignTeam findById(UUID campaignTeamID) {
        return campaignTeamRepository.findById(campaignTeamID).orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đội cứu hộ cho với ID: " + campaignTeamID));
    }

    @Transactional
    public TeamResponse createTeam(UUID campaignId, String teamName, List<String> vehicles,
            UUID leaderId, UUID creatorId, TeamApprovalStatus initialApprovalStatus) {
        if (creatorId == null || leaderId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Thiếu thông tin người tạo hoặc trưởng đội");
        }

        // Lấy khóa phân tán Redis theo leaderId (khóa 10s để chống click đúp / spam thao tác)
        String lockKey = "lock:team:create:" + leaderId;
        boolean acquired = redisService.acquireLock(lockKey, 10);
        if (!acquired) {
            throw new BusinessException(ErrorCode.CONFLICT, "Yêu cầu tạo đội đang được xử lý");
        }

        try {
            Campaign campaign = campaignService.findById(campaignId);
            if (campaign.getStatus() != CampaignStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Chiến dịch cứu hộ đã kết thúc");
            }

            if (campaignMemberService.hasActiveAcceptedMembership(leaderId)) {
                throw new BusinessException(ErrorCode.CONFLICT, "Người dùng hiện đang tham gia một đội cứu hộ khác");
            }
            
            UserInfoGrpcResponse leaderInfo = userGrpcClient.getUserById(leaderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin đội trưởng"));

            if (!"RESCUER".equalsIgnoreCase(leaderInfo.getRole())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Người được chọn làm Đội trưởng phải là Người cứu hộ");
            }

            CampaignTeam team = CampaignTeam.builder()
                    .campaign(campaign)
                    .teamName(teamName == null ? null : teamName.trim())
                    .vehicles(vehicles == null ? new ArrayList<>() : new ArrayList<>(vehicles))
                    .leaderId(leaderId)
                    .leaderPhone(leaderInfo.getPhoneNumber())
                    .approvalStatus(initialApprovalStatus)
                    .build();
            if (initialApprovalStatus == TeamApprovalStatus.APPROVED) {
                team.setReviewedBy(creatorId);
                team.setReviewedAt(LocalDateTime.now());
            }
            CampaignTeam savedTeam = campaignTeamRepository.save(team);

            campaignMemberService.createLeaderMember(savedTeam, leaderInfo);

            return campaignTeamMapper.toResponse(savedTeam);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "Người dùng hiện đang hoạt động trong một đội cứu hộ khác");
        } finally {
            redisService.releaseLock(lockKey);
        }
    }

    @Transactional
    public TeamResponse approveTeam(UUID teamId, UUID reviewerId) {
        CampaignTeam team = findPendingTeamForReview(teamId);
        if (team.getCampaign().getStatus() != CampaignStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "Chiến dịch không còn hoạt động, không thể duyệt đội");
        }

        team.setApprovalStatus(TeamApprovalStatus.APPROVED);
        team.setReviewedAt(LocalDateTime.now());
        team.setReviewedBy(reviewerId);
        team.setRejectionReason(null);
        return campaignTeamMapper.toResponse(team);
    }

    @Transactional
    public TeamResponse rejectTeam(UUID teamId, UUID reviewerId, RejectTeamRequest request) {
        CampaignTeam team = findPendingTeamForReview(teamId);
        team.setApprovalStatus(TeamApprovalStatus.REJECTED);
        team.setReviewedAt(LocalDateTime.now());
        team.setReviewedBy(reviewerId);
        team.setRejectionReason(request.reason().trim());
        campaignMemberService.cancelLeaderMembership(team);
        return campaignTeamMapper.toResponse(team);
    }

    private CampaignTeam findPendingTeamForReview(UUID teamId) {
        CampaignTeam team = campaignTeamRepository.findByIdForReview(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đội cứu hộ với ID: " + teamId));
        if (team.getApprovalStatus() != TeamApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "Yêu cầu thành lập đội đã được xét duyệt");
        }
        if (team.getStatus() == TeamStatus.DISBANDED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Đội đã giải tán, không thể xét duyệt");
        }
        return team;
    }

    /**
     * "Off team" (Giải tán / Kết thúc đợt cứu trợ của đội)
     */
    @Transactional
    public TeamResponse disbandTeam(UUID teamId, UUID leaderId) {
        CampaignTeam team = findById(teamId);

        if (leaderId != null && !team.getLeaderId().equals(leaderId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền giải tán đội cứu hộ này");
        }

        if (team.getStatus() == TeamStatus.DISBANDED) {
            return campaignTeamMapper.toResponse(team);
        }

        if (team.getStatus() == TeamStatus.BUSY) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Đội cứu hộ đang trong trạng thái BUSY thực hiện nhiệm vụ, không thể giải tán đột ngột. Vui lòng hoàn thành nhiệm vụ trước.");
        }

        team.setStatus(TeamStatus.DISBANDED);
        CampaignTeam updatedTeam = campaignTeamRepository.save(team);

        // Xóa vị trí realtime khỏi Redis
        redisService.deleteLocation(teamId);

        return campaignTeamMapper.toResponse(updatedTeam);
    }
    
    /**
     * Lấy thông tin đội đang hoạt động của Đội trưởng hiện tại
     */
    @Transactional(readOnly = true)
    public TeamResponse getMyActiveTeam(UUID leaderId) {
        if (leaderId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu thông tin người dùng");
        }

        CampaignTeam team = campaignTeamRepository.findFirstByLeaderIdAndStatusNotAndApprovalStatusNot(
                leaderId, TeamStatus.DISBANDED, TeamApprovalStatus.REJECTED)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Bạn hiện không quản lý đội cứu hộ nào đang hoạt động"));

        return campaignTeamMapper.toResponse(team);
    }

    /**
     * Lấy chi tiết thông tin đội cứu hộ theo ID
     */
    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID id) {
        CampaignTeam team = findById(id);
        return campaignTeamMapper.toResponse(team);
    }

    /**
     * Cập nhật thông tin Đội Cứu hộ theo ID (Cập nhật linh hoạt - Partial Update)
     * Chỉ cập nhật các trường được truyền lên (khác null).
     */
    @Transactional
    public TeamResponse updateTeam(UUID id, UpdateTeamRequest request) {
        CampaignTeam existingTeam = campaignTeamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đội cứu hộ với ID: " + id));

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
        return campaignTeamMapper.toResponse(updatedTeam);
    }

    /**
     * Truy vấn thông tin đội cứu hộ trong chiến dịch hiện tại theo Leader ID (phục vụ gRPC/nội bộ)
     */
    public CampaignTeam getActiveTeamByLeaderId(UUID leaderId) {
        return campaignTeamRepository.findActiveTeamByLeaderId(leaderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đội cứu hộ cho Leader ID: " + leaderId));
    }

    public void saveTeamLocation(UUID leaderId, TeamLocationRequest request) {
        CampaignTeam team = this.getActiveTeamByLeaderId(leaderId);

        TeamLocation teamLocation = TeamLocation.builder()
                .latitude(request.latitude())
                .longitude(request.longitude())
                .speed(request.speed())
                .heading(request.heading())
                .recordedAt(Instant.now())
                .teamName(team.getTeamName())
                .leaderId(team.getLeaderId().toString())
                .leaderPhone(team.getLeaderPhone())
                .build();

        redisService.saveLocation(team.getId(), teamLocation);

        locationEventProducer.publishTeamLocationUpdateEvent(new TeamLocationUpdatedEvent(
                team.getId(),
                teamLocation.latitude(),
                teamLocation.longitude(),
                teamLocation.speed(),
                teamLocation.heading(),
                teamLocation.recordedAt()));
    }

    /**
     * Lấy tọa độ vị trí mới nhất của đội cứu hộ từ Redis
     */
    public TeamLocation getTeamLocation(UUID teamId) {
        TeamLocation location = redisService.getCurrentLocation(teamId);
        if (location == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chưa có thông tin vị trí gần đây của đội cứu hộ: " + teamId);
        }
        return location;
    }

    /**
     * Chuyển trạng thái đội sang READY
     */
    @Transactional
    public void setTeamReady(UUID campaignTeamId) {
        if (campaignTeamId == null) {
            return;
        }

        CampaignTeam team = findById(campaignTeamId);
        if (team.getStatus() == TeamStatus.READY) {
            return;
        }

        team.setStatus(TeamStatus.READY);
    }

    public boolean isLeaderOfTeam(UUID leaderId, UUID teamId) {
        if (leaderId == null || teamId == null) {
            return false;
        }
        return campaignTeamRepository.existsByIdAndLeaderId(teamId, leaderId);
    }

    public List<TeamLocation> getAllTeamLocations() {
        return redisService.getAllLocations();
    }

    public List<CampaignTeam> getTeamsLocationsNearby(Double latitude, Double longitude, Integer radiusInMeters) {
        List<UUID> nearbyUserIds = redisService.findTeamIdRadius(longitude, latitude, radiusInMeters)
                .stream()
                .map(UUID::fromString)
                .filter(Objects::nonNull)
                .toList();

        if (nearbyUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        return campaignTeamRepository.findByIdInAndStatus(nearbyUserIds, TeamStatus.READY);
    }
}
