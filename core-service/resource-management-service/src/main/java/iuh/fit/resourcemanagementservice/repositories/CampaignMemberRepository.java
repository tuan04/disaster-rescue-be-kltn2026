package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.dtos.response.TeamMemberUserResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignMemberRepository extends JpaRepository<CampaignMember, UUID> {

    boolean existsByCampaignTeam_IdAndMemberId(UUID campaignTeamId, UUID memberId);

    Optional<CampaignMember> findByCampaignTeam_IdAndMemberId(UUID campaignTeamId, UUID memberId);

    void deleteByCampaignTeam_IdAndMemberId(UUID campaignTeamId, UUID memberId);


    @Query("""
        SELECT
            member.campaignTeam.id AS teamId,
            member.memberId AS userId
        FROM CampaignMember member
        WHERE member.campaignTeam.id IN :teamIds
        """)
    List<TeamMemberUserResponse> findRecipientUsersByTeamIds(
            @Param("teamIds") Collection<UUID> teamIds
    );

}
