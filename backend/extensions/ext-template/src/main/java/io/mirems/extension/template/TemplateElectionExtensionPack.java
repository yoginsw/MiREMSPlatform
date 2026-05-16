package io.mirems.extension.template;

import io.mirems.core.domain.extension.ElectionExtensionPack;
import java.util.List;
import java.util.Map;

/**
 * Template implementation copied when creating a new country extension pack.
 */
public final class TemplateElectionExtensionPack implements ElectionExtensionPack {
    @Override
    public String id() {
        return "template";
    }

    @Override
    public String countryCode() {
        return "XX";
    }

    @Override
    public String displayName() {
        return "Template Election Extension Pack";
    }

    @Override
    public List<String> flywayMigrationLocations() {
        return List.of("classpath:db/migration/ext/template");
    }

    @Override
    public List<String> processResourceLocations() {
        return List.of("classpath:processes/ext/template");
    }

    @Override
    public Map<String, String> metadata() {
        return Map.of("template", "true");
    }
}
