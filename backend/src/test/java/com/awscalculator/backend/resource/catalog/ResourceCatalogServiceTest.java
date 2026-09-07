package com.awscalculator.backend.resource.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.awscalculator.backend.resource.ResourceType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceCatalogServiceTest {

    @Mock
    private ResourceCatalogEntryRepository resourceCatalogEntryRepository;

    @Mock
    private ResourceCatalogFieldOptionRepository fieldOptionRepository;

    @InjectMocks
    private ResourceCatalogService resourceCatalogService;

    @Test
    void itemsBuildsFieldOptionsFromNormalizedOptionRows() {
        ResourceCatalogEntry entry = entryWithFields(List.of(
                selectField("instanceType"),
                numberField("storageGb")
        ));
        entry.setId(10L);
        ResourceCatalogFieldOption micro = new ResourceCatalogFieldOption(
                entry, "instanceType", "db.t4g.micro", "db.t4g.micro", 0);
        ResourceCatalogFieldOption small = new ResourceCatalogFieldOption(
                entry, "instanceType", "db.t4g.small", "db.t4g.small", 1);

        when(resourceCatalogEntryRepository.findAllByOrderByIdAsc()).thenReturn(List.of(entry));
        when(fieldOptionRepository.findAllByCatalogEntryIdInOrderByCatalogEntryIdAscFieldKeyAscOptionOrderAsc(List.of(10L)))
                .thenReturn(List.of(small, micro));

        List<ResourceCatalogItem> items = resourceCatalogService.items();

        ResourceCatalogField instanceType = items.getFirst().fields().getFirst();
        assertThat(instanceType.options())
                .extracting(ResourceCatalogOption::value)
                .containsExactly("db.t4g.micro", "db.t4g.small");
        assertThat(items.getFirst().fields().get(1).options()).isEmpty();
    }

    @Test
    void seedIfMissingStoresOptionsInSeparateTableAndStripsEmbeddedOptions() {
        ResourceCatalogEntry entry = entryWithFields(List.of(
                new ResourceCatalogField(
                        "instanceType",
                        "Instance Type",
                        "select",
                        "db.t4g.micro",
                        List.of(new ResourceCatalogOption("db.t4g.micro", "db.t4g.micro")),
                        null,
                        null,
                        null,
                        true
                )
        ));
        when(resourceCatalogEntryRepository.findByType(any(ResourceType.class))).thenReturn(Optional.of(entry));
        resourceCatalogService.seedIfMissing();

        ArgumentCaptor<ResourceCatalogFieldOption> optionCaptor = ArgumentCaptor.forClass(ResourceCatalogFieldOption.class);
        verify(fieldOptionRepository, org.mockito.Mockito.atLeastOnce()).deleteByCatalogEntry(any(ResourceCatalogEntry.class));
        verify(fieldOptionRepository, org.mockito.Mockito.atLeastOnce()).flush();
        verify(fieldOptionRepository, org.mockito.Mockito.atLeastOnce()).save(optionCaptor.capture());
        assertThat(optionCaptor.getAllValues())
                .anySatisfy(option -> {
                    assertThat(option.getFieldKey()).isEqualTo("instanceType");
                    assertThat(option.getValue()).isEqualTo("db.t4g.micro");
                });
        assertThat(entry.getFields().getFirst().options()).isEmpty();
    }

    private ResourceCatalogEntry entryWithFields(List<ResourceCatalogField> fields) {
        return new ResourceCatalogEntry(
                ResourceType.RDS,
                "rds",
                "RDS",
                "RDS",
                "Database",
                "관계형 DB",
                "bg-blue-100",
                Map.of("instanceType", "db.t4g.micro"),
                fields
        );
    }

    private ResourceCatalogField selectField(String key) {
        return new ResourceCatalogField(key, key, "select", "", List.of(), null, null, null, true);
    }

    private ResourceCatalogField numberField(String key) {
        return new ResourceCatalogField(key, key, "number", 1, List.of(), 0, null, 1, true);
    }
}
