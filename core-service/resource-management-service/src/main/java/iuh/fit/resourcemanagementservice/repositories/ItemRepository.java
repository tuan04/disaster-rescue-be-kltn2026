package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    @Query(value = "SELECT nextval('item_code_seq')", nativeQuery = true)
    long nextCodeSequence();

    @Query("""
            SELECT i FROM Item i WHERE i.isDeleted = false
              AND (locate(lower(:keyword), lower(i.name)) > 0
                OR locate(lower(:keyword), lower(i.code)) > 0)
            """)
    Page<Item> searchByNameOrCode(@Param("keyword") String keyword, Pageable pageable);

    Optional<Item> findByIdAndIsDeletedFalse(UUID id);

    Optional<Item> findByNameIgnoreCaseAndIsDeletedFalse(String name);

    Optional<Item> findByNameIgnoreCase(String name);

    Page<Item> findByIsDeletedFalse(Pageable pageable);

    Page<Item> findByNameContainingIgnoreCaseAndIsDeletedFalse(String keyword, Pageable pageable);
}
