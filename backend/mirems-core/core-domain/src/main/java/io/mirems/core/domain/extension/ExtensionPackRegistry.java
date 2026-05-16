package io.mirems.core.domain.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable registry of enabled election extension packs discovered at runtime.
 */
public final class ExtensionPackRegistry {
    private final Map<String, ElectionExtensionPack> packsById;

    public ExtensionPackRegistry(Collection<? extends ElectionExtensionPack> extensionPacks) {
        Objects.requireNonNull(extensionPacks, "extensionPacks must not be null");
        Map<String, ElectionExtensionPack> registered = new LinkedHashMap<>();
        for (ElectionExtensionPack pack : extensionPacks) {
            ElectionExtensionPack nonNullPack = Objects.requireNonNull(pack, "extension pack must not be null");
            String id = normalizeId(nonNullPack.id());
            if (registered.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate extension pack id: " + id);
            }
            registered.put(id, nonNullPack);
        }
        this.packsById = Collections.unmodifiableMap(new LinkedHashMap<>(registered));
    }

    public Optional<ElectionExtensionPack> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(packsById.get(id.trim()));
    }

    public ElectionExtensionPack requireById(String id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown extension pack id: " + id));
    }

    public List<String> enabledPackIds() {
        return List.copyOf(new ArrayList<>(packsById.keySet()));
    }

    public List<ElectionExtensionPack> enabledPacks() {
        return List.copyOf(packsById.values());
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Extension pack id must not be blank");
        }
        return id.trim();
    }
}
