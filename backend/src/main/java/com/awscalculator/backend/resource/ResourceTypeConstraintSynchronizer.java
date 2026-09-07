package com.awscalculator.backend.resource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ResourceTypeConstraintSynchronizer implements ApplicationRunner {

    private static final List<ResourceTypeConstraint> CONSTRAINTS = List.of(
            new ResourceTypeConstraint("resources", "type", "resources_type_check"),
            new ResourceTypeConstraint("resource_catalog_entries", "type", "resource_catalog_entries_type_check"),
            new ResourceTypeConstraint("resource_pricing_rules", "resource_type", "resource_pricing_rules_resource_type_check")
    );
    private static final String FIND_TYPE_CHECK_CONSTRAINTS = """
            select c.conname
            from pg_constraint c
            join pg_class t on t.oid = c.conrelid
            join pg_namespace n on n.oid = t.relnamespace
            where c.contype = 'c'
              and t.relname = ?
              and n.nspname = current_schema()
              and pg_get_constraintdef(c.oid) like ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (ResourceTypeConstraint constraint : CONSTRAINTS) {
            synchronize(constraint);
        }
    }

    private void synchronize(ResourceTypeConstraint constraint) {
        List<String> constraintNames = jdbcTemplate.queryForList(
                FIND_TYPE_CHECK_CONSTRAINTS,
                String.class,
                constraint.tableName(),
                "%" + constraint.columnName() + "%"
        );
        for (String constraintName : constraintNames) {
            jdbcTemplate.update("alter table " + quoteIdentifier(constraint.tableName())
                    + " drop constraint " + quoteIdentifier(constraintName));
        }
        jdbcTemplate.update("alter table " + quoteIdentifier(constraint.tableName())
                + " add constraint " + quoteIdentifier(constraint.constraintName())
                + " check (" + quoteIdentifier(constraint.columnName()) + " in (" + allowedResourceTypes() + "))");
    }

    static String allowedResourceTypes() {
        return Arrays.stream(ResourceType.values())
                .map(ResourceType::name)
                .map(ResourceTypeConstraintSynchronizer::quoteLiteral)
                .collect(Collectors.joining(", "));
    }

    static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String quoteLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private record ResourceTypeConstraint(String tableName, String columnName, String constraintName) {
    }
}
