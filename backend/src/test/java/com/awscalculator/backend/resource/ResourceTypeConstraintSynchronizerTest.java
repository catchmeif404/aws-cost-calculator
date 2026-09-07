package com.awscalculator.backend.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ResourceTypeConstraintSynchronizerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ResourceTypeConstraintSynchronizer synchronizer;

    @Test
    void allowedResourceTypesContainsEveryResourceType() {
        String allowedResourceTypes = ResourceTypeConstraintSynchronizer.allowedResourceTypes();

        for (ResourceType resourceType : ResourceType.values()) {
            assertThat(allowedResourceTypes).contains("'" + resourceType.name() + "'");
        }
    }

    @Test
    void runRecreatesTypeCheckConstraint() {
        when(jdbcTemplate.queryForList(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(String.class),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of("resources_type_check"));

        synchronizer.run(null);

        verify(jdbcTemplate).update("alter table \"resources\" drop constraint \"resources_type_check\"");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, org.mockito.Mockito.times(6)).update(sqlCaptor.capture());
        assertThat(sqlCaptor.getAllValues().get(1))
                .startsWith("alter table \"resources\" add constraint \"resources_type_check\" check (\"type\" in (")
                .contains("'AURORA'", "'OPENSEARCH'", "'SECRETS_MANAGER'", "'ROUTE53'");
        assertThat(sqlCaptor.getAllValues().get(3))
                .startsWith("alter table \"resource_catalog_entries\" add constraint \"resource_catalog_entries_type_check\"");
        assertThat(sqlCaptor.getAllValues().get(5))
                .startsWith("alter table \"resource_pricing_rules\" add constraint \"resource_pricing_rules_resource_type_check\"");
    }
}
