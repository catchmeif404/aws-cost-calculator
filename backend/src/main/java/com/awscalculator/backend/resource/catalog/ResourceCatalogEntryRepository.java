package com.awscalculator.backend.resource.catalog;

import com.awscalculator.backend.resource.ResourceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceCatalogEntryRepository extends JpaRepository<ResourceCatalogEntry, Long> {
    boolean existsByType(ResourceType type);

    Optional<ResourceCatalogEntry> findByType(ResourceType type);

    List<ResourceCatalogEntry> findAllByOrderByIdAsc();
}
