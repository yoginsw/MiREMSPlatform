package io.mirems.extension.us;

import java.util.Objects;
import java.util.UUID;

public final class UsElectoralDistrict {
    private final UUID id;
    private final String districtCode;
    private final String name;
    private final UsJurisdiction jurisdiction;
    private final int seats;
    private final UsDistrictType type;

    private UsElectoralDistrict(
            UUID id, String districtCode, String name, UsJurisdiction jurisdiction, int seats, UsDistrictType type) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.districtCode = requireText(districtCode, "districtCode");
        this.name = requireText(name, "name");
        this.jurisdiction = requireJurisdiction(jurisdiction);
        this.type = Objects.requireNonNull(type, "type is required");
        this.seats = validateSeats(seats, this.type);
    }

    public static UsElectoralDistrict singleMember(
            UUID id, String districtCode, String name, UsJurisdiction jurisdiction) {
        return new UsElectoralDistrict(id, districtCode, name, jurisdiction, 1, UsDistrictType.STATE_LEGISLATURE);
    }

    public static UsElectoralDistrict multiMember(
            UUID id, String districtCode, String name, UsJurisdiction jurisdiction, int seats) {
        return new UsElectoralDistrict(id, districtCode, name, jurisdiction, seats, UsDistrictType.LOCAL_AT_LARGE);
    }

    public UUID id() {
        return id;
    }

    public String districtCode() {
        return districtCode;
    }

    public String name() {
        return name;
    }

    public UsJurisdiction jurisdiction() {
        return jurisdiction;
    }

    public int seats() {
        return seats;
    }

    public UsDistrictType type() {
        return type;
    }

    public boolean isMultiMember() {
        return seats > 1;
    }

    private static int validateSeats(int seats, UsDistrictType type) {
        if (type == UsDistrictType.LOCAL_AT_LARGE && seats < 2) {
            throw new IllegalArgumentException("multi-member district must have at least 2 seats");
        }
        if (seats < 1) {
            throw new IllegalArgumentException("district seats must be positive");
        }
        if (seats > 99) {
            throw new IllegalArgumentException("district seats must not exceed 99");
        }
        return seats;
    }

    private static UsJurisdiction requireJurisdiction(UsJurisdiction jurisdiction) {
        if (jurisdiction == null) {
            throw new IllegalArgumentException("jurisdiction is required");
        }
        return jurisdiction;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
