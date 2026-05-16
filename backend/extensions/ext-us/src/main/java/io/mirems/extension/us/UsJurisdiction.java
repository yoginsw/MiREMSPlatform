package io.mirems.extension.us;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class UsJurisdiction {
    private static final String STATE_FIPS_PATTERN = "\\d{2}";
    private static final String COUNTY_FIPS_PATTERN = "\\d{5}";
    private static final String PRECINCT_MAPPING_PATTERN = "\\d{5}-[A-Za-z0-9][A-Za-z0-9_-]*";

    private final UUID id;
    private final String fipsCode;
    private final String name;
    private final UsJurisdictionLevel level;
    private final UsJurisdiction parent;
    private final String precinctCode;

    private UsJurisdiction(
            UUID id,
            String fipsCode,
            String name,
            UsJurisdictionLevel level,
            UsJurisdiction parent,
            String precinctCode) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.fipsCode = requireText(fipsCode, "fipsCode");
        this.name = requireText(name, "name");
        this.level = Objects.requireNonNull(level, "level is required");
        this.parent = parent;
        this.precinctCode = precinctCode;
        validateFipsAndParent();
    }

    public static UsJurisdiction state(UUID id, String stateFipsCode, String name) {
        return new UsJurisdiction(id, stateFipsCode, name, UsJurisdictionLevel.STATE, null, null);
    }

    public static UsJurisdiction county(UUID id, String countyFipsCode, String name, UsJurisdiction parent) {
        return new UsJurisdiction(id, countyFipsCode, name, UsJurisdictionLevel.COUNTY, parent, null);
    }

    public static UsJurisdiction precinct(UUID id, String precinctMappingCode, String name, UsJurisdiction parent) {
        String normalizedMapping = requireText(precinctMappingCode, "precinctMappingCode");
        String[] parts = normalizedMapping.split("-", 2);
        return new UsJurisdiction(id, parts[0], name, UsJurisdictionLevel.PRECINCT, parent, parts.length == 2 ? parts[1] : "");
    }

    public UUID id() {
        return id;
    }

    public String fipsCode() {
        return fipsCode;
    }

    public String countyFipsCode() {
        return fipsCode;
    }

    public String name() {
        return name;
    }

    public UsJurisdictionLevel level() {
        return level;
    }

    public Optional<UsJurisdiction> parent() {
        return Optional.ofNullable(parent);
    }

    public String precinctCode() {
        if (level != UsJurisdictionLevel.PRECINCT) {
            throw new IllegalStateException("precinctCode is only available for PRECINCT jurisdictions");
        }
        return precinctCode;
    }

    public String hierarchyPath() {
        return parent == null ? name : parent.hierarchyPath() + " > " + name;
    }

    private void validateFipsAndParent() {
        switch (level) {
            case STATE -> validateState();
            case COUNTY -> validateCounty();
            case PRECINCT -> validatePrecinct();
        }
    }

    private void validateState() {
        if (parent != null) {
            throw new IllegalArgumentException("STATE must not have a parent");
        }
        if (!fipsCode.matches(STATE_FIPS_PATTERN)) {
            throw new IllegalArgumentException("state FIPS must be exactly 2 digits");
        }
    }

    private void validateCounty() {
        if (parent == null || parent.level() != UsJurisdictionLevel.STATE) {
            throw new IllegalArgumentException("COUNTY parent must be STATE");
        }
        if (!fipsCode.matches(COUNTY_FIPS_PATTERN)) {
            throw new IllegalArgumentException("county FIPS must be exactly 5 digits");
        }
        if (!fipsCode.startsWith(parent.fipsCode())) {
            throw new IllegalArgumentException("county FIPS must start with parent state FIPS");
        }
    }

    private void validatePrecinct() {
        if (parent == null || parent.level() != UsJurisdictionLevel.COUNTY) {
            throw new IllegalArgumentException("PRECINCT parent must be COUNTY");
        }
        String mappingCode = fipsCode + "-" + precinctCode;
        if (!mappingCode.matches(PRECINCT_MAPPING_PATTERN)) {
            throw new IllegalArgumentException("precinct mapping must be <county FIPS>-<precinct code>");
        }
        if (!fipsCode.equals(parent.fipsCode())) {
            throw new IllegalArgumentException("precinct mapping must start with parent county FIPS");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
