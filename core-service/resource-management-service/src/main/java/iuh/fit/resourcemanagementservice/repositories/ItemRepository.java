package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    Optional<Item> findByIdAndIsDeletedFalse(UUID id);

    Optional<Item> findByNameIgnoreCaseAndIsDeletedFalse(String name);

    Optional<Item> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);
}
