package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.Campaign;
import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    @Query(value = "SELECT nextval('campaign_code_seq')", nativeQuery = true)
    long nextCodeSequence();

    @Query("""
            SELECT c FROM Campaign c
            WHERE locate(lower(:keyword), lower(c.name)) > 0
               OR locate(lower(:keyword), lower(c.code)) > 0
            """)
    Page<Campaign> searchByNameOrCode(@Param("keyword") String keyword, Pageable pageable);

    List<Campaign> findByStatus(CampaignStatus status);
}
