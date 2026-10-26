package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.kafka.dto.TeamLocationUpdatedEvent;
import iuh.fit.resourcemanagementservice.dtos.redis.TeamLocation;
import iuh.fit.resourcemanagementservice.dtos.request.RejectTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.request.TeamLocationRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamResponse;
import iuh.fit.resourcemanagementservice.dtos.response.TeamSummaryResponse;
import iuh.fit.resourcemanagementservice.entity.Campaign;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus;
import iuh.fit.resourcemanagementservice.events.producer.LocationEventProducer;
import iuh.fit.resourcemanagementservice.repositories.CampaignTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import iuh.fit.common.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import iuh.fit.resourcemanagementservice.specifications.CampaignTeamSpecification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    public CampaignTeam findById(UUID campaignTeamID) {
        return campaignTeamRepository.findById(campaignTeamID).orElseThrow(() ->
                new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đội cứu hộ cho với ID: " + campaignTeamID));
    }

    public TeamResponse createTeam(UUID campaignId, String teamName, List<String> vehicles,
            UUID leaderId, UUID creatorId, TeamApprovalStatus initialApprovalStatus) {
        if (creatorId == null || leaderId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Thiếu thông tin người tạo hoặc trưởng đội");
        }

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

            if (campaignTeamRepository.existsByCampaign_IdAndTeamNameIgnoreCaseAndStatusNotAndApprovalStatusNot(
                    campaignId, teamName, TeamStatus.DISBANDED, TeamApprovalStatus.REJECTED)) {
                throw new BusinessException(ErrorCode.CONFLICT, "Tên đội cứu hộ đã tồn tại");
            }

            if (campaignMemberService.hasActiveAcceptedMembership(leaderId)) {
                throw new BusinessException(ErrorCode.CONFLICT, "Người dùng hiện đang tham gia một đội cứu hộ khác");
            }

            UserInfoGrpcResponse leaderInfo = userGrpcClient.getUserById(leaderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin đội trưởng"));

            if (!"RESCUER".equalsIgnoreCase(leaderInfo.getRole())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Người được chọn làm Đội trưởng phải là Người cứu hộ");
            }

            return transactionTemplate.execute(status -> {
                CampaignTeam team = CampaignTeam.builder()
                        .campaign(campaign)
                        .teamName(teamName == null ? null : teamName.trim())
                        .vehicles(vehicles == null ? new ArrayList<>() : new ArrayList<>(vehicles))
                        .leaderId(leaderId)
                        .leaderName(leaderInfo.getFullName())
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
            });
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

        if (leaderId == null || !team.getLeaderId().equals(leaderId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Không có quyền thực hiện thao tác này");
        }

        if (team.getStatus() == TeamStatus.DISBANDED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Đội cứu hộ đã giải tán");
        }

        if (team.getApprovalStatus() != TeamApprovalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Không thể giải tán đội cứu hộ chưa được phê duyệt");
        }

        if (team.getStatus() == TeamStatus.BUSY) {
            throw new BusinessException(ErrorCode.CONFLICT, "Không thể giải tán đội đang thực hiện nhiệm vụ.");
        }

        team.setStatus(TeamStatus.DISBANDED);
        CampaignTeam updatedTeam = campaignTeamRepository.save(team);

        redisService.deleteLocation(teamId);

        return campaignTeamMapper.toResponse(updatedTeam);
    }
    
    /**
     * Lấy thông tin đội đang hoạt động của người dùng hiện tại (áp dụng cho cả Đội trưởng và Cứu hộ viên)
     */
    @Transactional(readOnly = true)
    public TeamResponse getMyActiveTeam(UUID userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Thiếu thông tin người dùng");
        }

        CampaignTeam team = campaignTeamRepository.findFirstByLeaderIdAndStatusNotAndApprovalStatusNot(
                userId, TeamStatus.DISBANDED, TeamApprovalStatus.REJECTED)
                .or(() -> campaignMemberService.findActiveTeamByMemberId(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Bạn hiện không tham gia đội cứu hộ nào đang hoạt động"));

        return campaignTeamMapper.toResponse(team);
    }

    /**
     * Lấy danh sách đội cứu hộ (hỗ trợ phân trang, lọc theo chiến dịch, trạng thái duyệt, trạng thái hoạt động, tìm kiếm)
     */
    @Transactional(readOnly = true)
    public PageResponse<TeamSummaryResponse> getTeams(
            UUID campaignId,
            TeamApprovalStatus approvalStatus,
            TeamStatus status,
            String keyword,
            Pageable pageable
    ) {
        Specification<CampaignTeam> spec = Specification
                .where(CampaignTeamSpecification.hasCampaignId(campaignId))
                .and(CampaignTeamSpecification.hasApprovalStatus(approvalStatus))
                .and(CampaignTeamSpecification.hasStatus(status))
                .and(CampaignTeamSpecification.hasKeyword(keyword));

        Page<CampaignTeam> teamPage = campaignTeamRepository.findAll(spec, pageable);
        Page<TeamSummaryResponse> responsePage = teamPage.map(campaignTeamMapper::toSummaryResponse);
        return PageResponse.from(responsePage);
    }

    /**
     * Lấy danh sách đội cứu hộ đang hoạt động trong các chiến dịch hiện tại
     */
    @Transactional(readOnly = true)
    public PageResponse<TeamSummaryResponse> getActiveTeams(String keyword, Pageable pageable) {
        Specification<CampaignTeam> spec = Specification
                .where(CampaignTeamSpecification.inActiveCampaign())
                .and(CampaignTeamSpecification.hasApprovalStatus(TeamApprovalStatus.APPROVED))
                .and(CampaignTeamSpecification.notDisbanded())
                .and(CampaignTeamSpecification.hasKeyword(keyword));

        Page<CampaignTeam> teamPage = campaignTeamRepository.findAll(spec, pageable);
        Page<TeamSummaryResponse> responsePage = teamPage.map(campaignTeamMapper::toSummaryResponse);
        return PageResponse.from(responsePage);
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
