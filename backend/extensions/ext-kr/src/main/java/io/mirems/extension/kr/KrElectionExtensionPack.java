package io.mirems.extension.kr;

import io.mirems.core.domain.extension.ElectionExtensionPack;
import java.util.List;
import java.util.Map;

public final class KrElectionExtensionPack implements ElectionExtensionPack {
    private static final List<String> FLYWAY_MIGRATION_LOCATIONS = List.of("classpath:db/migration/ext/kr");
    private static final List<String> PROCESS_RESOURCE_LOCATIONS = List.of("classpath:processes/ext/kr");
    private static final Map<String, String> METADATA = Map.of(
            "legalBasisDocument", "docs/extensions/kr/LEGAL.md",
            "extensionPhase", "P6");

    @Override
    public String id() {
        return "kr";
    }

    @Override
    public String countryCode() {
        return "KR";
    }

    @Override
    public String displayName() {
        return "대한민국 선거 확장팩";
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
