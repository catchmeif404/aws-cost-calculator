package com.awscalculator.backend.resource.catalog;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceCatalogFieldOptionRepository extends JpaRepository<ResourceCatalogFieldOption, Long> {
    boolean existsByCatalogEntryAndFieldKeyAndOptionOrder(
            ResourceCatalogEntry catalogEntry,
            String fieldKey,
            int optionOrder
    );

    void deleteByCatalogEntry(ResourceCatalogEntry catalogEntry);

    List<ResourceCatalogFieldOption> findAllByCatalogEntryIdInOrderByCatalogEntryIdAscFieldKeyAscOptionOrderAsc(
            Collection<Long> catalogEntryIds
    );
}
