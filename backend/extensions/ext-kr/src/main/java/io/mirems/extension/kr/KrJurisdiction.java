package io.mirems.extension.kr;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class KrJurisdiction {
    private final UUID id;
    private final String administrativeCode;
    private final String name;
    private final String constituencyCode;
    private final KrJurisdictionLevel level;
    private final KrJurisdiction parent;

    private KrJurisdiction(
            UUID id,
            String administrativeCode,
            String name,
            String constituencyCode,
            KrJurisdictionLevel level,
            KrJurisdiction parent) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.administrativeCode = requireText(administrativeCode, "administrativeCode");
        this.name = requireText(name, "name");
        this.constituencyCode = requireText(constituencyCode, "constituencyCode");
        this.level = Objects.requireNonNull(level, "level is required");
        this.parent = parent;
        validateParent(level, parent);
    }

    public static KrJurisdiction sido(UUID id, String administrativeCode, String name, String constituencyCode) {
        return new KrJurisdiction(id, administrativeCode, name, constituencyCode, KrJurisdictionLevel.SIDO, null);
    }

    public static KrJurisdiction sigungu(
            UUID id, String administrativeCode, String name, String constituencyCode, KrJurisdiction parent) {
        return new KrJurisdiction(id, administrativeCode, name, constituencyCode, KrJurisdictionLevel.SIGUNGU, parent);
    }

    public static KrJurisdiction eupMyeonDong(
            UUID id, String administrativeCode, String name, String constituencyCode, KrJurisdiction parent) {
        return new KrJurisdiction(id, administrativeCode, name, constituencyCode, KrJurisdictionLevel.EUPMYEONDONG, parent);
    }

    public UUID id() {
        return id;
    }

    public String administrativeCode() {
        return administrativeCode;
    }

    public String name() {
        return name;
    }

    public String constituencyCode() {
        return constituencyCode;
    }

    public KrJurisdictionLevel level() {
        return level;
    }

    public Optional<KrJurisdiction> parent() {
        return Optional.ofNullable(parent);
    }

    public String hierarchyPath() {
        return parent == null ? name : parent.hierarchyPath() + " > " + name;
    }

    private static void validateParent(KrJurisdictionLevel level, KrJurisdiction parent) {
        switch (level) {
            case SIDO -> {
                if (parent != null) {
                    throw new IllegalArgumentException("SIDO must not have a parent");
                }
            }
            case SIGUNGU -> {
                if (parent == null || parent.level() != KrJurisdictionLevel.SIDO) {
                    throw new IllegalArgumentException("SIGUNGU parent must be SIDO");
                }
            }
            case EUPMYEONDONG -> {
                if (parent == null || parent.level() != KrJurisdictionLevel.SIGUNGU) {
                    throw new IllegalArgumentException("EUPMYEONDONG parent must be SIGUNGU");
                }
            }
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
