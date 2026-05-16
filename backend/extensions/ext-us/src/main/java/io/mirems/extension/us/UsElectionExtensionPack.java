package io.mirems.extension.us;

import io.mirems.core.domain.extension.ElectionExtensionPack;
import java.util.List;
import java.util.Map;

public final class UsElectionExtensionPack implements ElectionExtensionPack {
    private static final List<String> FLYWAY_MIGRATION_LOCATIONS = List.of("classpath:db/migration/ext/us");
    private static final List<String> PROCESS_RESOURCE_LOCATIONS = List.of("classpath:processes/ext/us");
    private static final Map<String, String> METADATA = Map.of(
            "legalBasisDocument", "docs/extensions/us/LEGAL.md",
            "extensionPhase", "P7");

    @Override
    public String id() {
        return "us";
    }

    @Override
    public String countryCode() {
        return "US";
    }

    @Override
    public String displayName() {
        return "United States election extension pack";
    }

    @Override
    public List<String> flywayMigrationLocations() {
        return FLYWAY_MIGRATION_LOCATIONS;
    }

    @Override
    public List<String> processResourceLocations() {
        return PROCESS_RESOURCE_LOCATIONS;
    }

    @Override
    public Map<String, String> metadata() {
        return METADATA;
    }
}
