package io.mirems.core.domain.extension;

import java.util.List;
import java.util.Map;

/**
 * Stable SPI implemented by country or jurisdiction-specific extension packs.
 * Core code depends only on this interface, never on a concrete ext-* module.
 */
public interface ElectionExtensionPack {
    String id();

    String countryCode();

    String displayName();

    List<String> flywayMigrationLocations();

    default List<String> processResourceLocations() {
        return List.of();
    }

    default Map<String, String> metadata() {
        return Map.of();
    }
}
