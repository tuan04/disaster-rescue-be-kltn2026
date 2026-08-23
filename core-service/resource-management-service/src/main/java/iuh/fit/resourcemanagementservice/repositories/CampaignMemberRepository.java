package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignMemberRepository extends JpaRepository<CampaignMember, UUID> {

    boolean existsByCampaignTeam_IdAndMemberId(UUID campaignTeamId, UUID memberId);

    Optional<CampaignMember> findByCampaignTeam_IdAndMemberId(UUID campaignTeamId, UUID memberId);

    void deleteByCampaignTeam_IdAndMemberId(UUID campaignTeamId, UUID memberId);
}
