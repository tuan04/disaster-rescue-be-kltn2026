package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.response.TeamMemberUserResponse;
import iuh.fit.resourcemanagementservice.dtos.response.TeamRecipientsResult;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.repositories.CampaignMemberRepository;
import iuh.fit.resourcemanagementservice.repositories.CampaignTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@RequiredArgsConstructor
public class TeamNotificationRecipientService {
    private final CampaignTeamRepository campaignTeamRepository;

    private final CampaignMemberRepository campaignMemberRepository;

    public List<TeamRecipientsResult> findRecipientsByTeamIds(
            List<UUID> teamIds
    ){

        if (teamIds == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Danh sách teamIds không được null"
            );
        }

        if (teamIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> uniqueTeamIds = new LinkedHashSet<>();
        for (UUID teamId : teamIds) {
            if (teamId == null) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "teamIds không được chứa phần tử null"
                );
            }
            uniqueTeamIds.add(teamId);
        }

        List<CampaignTeam> teams = campaignTeamRepository.findAllById(uniqueTeamIds);

        Map<UUID, Set<UUID>> recipientsByTeam = new LinkedHashMap<>();

        for (CampaignTeam team : teams) {
            Set<UUID> userIds = new LinkedHashSet<>();

            if (team.getLeaderId() != null) {
                userIds.add(team.getLeaderId());
            }

            recipientsByTeam.put(team.getId(), userIds);
        }

        if (!recipientsByTeam.isEmpty()) {

            // Truy vấn thành viên của tất cả đội tìm thấy trong một lần.
            List<TeamMemberUserResponse> members =
                    campaignMemberRepository.findRecipientUsersByTeamIds(
                            recipientsByTeam.keySet()
                    );

            for (TeamMemberUserResponse member : members) {
                Set<UUID> userIds =
                        recipientsByTeam.get(member.getTeamId());
                if (userIds != null && member.getUserId() != null) {
                    userIds.add(member.getUserId());
                }
            }
        }

        List<TeamRecipientsResult> results =
                new ArrayList<>(uniqueTeamIds.size());

        for (UUID teamId : uniqueTeamIds) {

            Set<UUID> userIds = recipientsByTeam.get(teamId);

            if (userIds == null) {

                results.add(
                        new TeamRecipientsResult(
                                teamId,
                                false,
                                List.of()
                        )
                );
            } else {
                // Có key nghĩa là đội tồn tại.
                results.add(
                        new TeamRecipientsResult(
                                teamId,
                                true,
                                List.copyOf(userIds)
                        )
                );
            }
        }

        return List.copyOf(results);
    }

}
