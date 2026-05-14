package io.mirems.core.domain.ballot;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Entity representing a jurisdiction/language/accessibility-specific ballot presentation. */
public class BallotStyle {
    private static final Set<String> ISO_639_1_LANGUAGES = Set.of(Locale.getISOLanguages());

    private final UUID id;
    private final Ballot ballot;
    private final String styleCode;
    private final String district;
    private final String language;
    private final Set<AccessibilityFeature> accessibilityFeatures;

    private BallotStyle(
            UUID id,
            Ballot ballot,
            String styleCode,
            String district,
            String language,
            Set<AccessibilityFeature> accessibilityFeatures) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.ballot = Objects.requireNonNull(ballot, "ballot is required");
        this.styleCode = Ballot.requireText(styleCode, "styleCode");
        this.district = Ballot.requireText(district, "district");
        this.language = validateLanguage(language);
        this.accessibilityFeatures = Set.copyOf(Objects.requireNonNull(
                accessibilityFeatures,
                "accessibilityFeatures is required"));
    }

    static BallotStyle create(
            UUID id,
            Ballot ballot,
            String styleCode,
            String district,
            String language,
            Set<AccessibilityFeature> accessibilityFeatures) {
        return new BallotStyle(id, ballot, styleCode, district, language, accessibilityFeatures);
    }

    public UUID getId() {
        return id;
    }

    public Ballot getBallot() {
        return ballot;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public String getDistrict() {
        return district;
    }

    public String getLanguage() {
        return language;
    }

    public Set<AccessibilityFeature> getAccessibilityFeatures() {
        return accessibilityFeatures;
    }

    private static String validateLanguage(String language) {
        String normalized = Ballot.requireText(language, "language").toLowerCase(Locale.ROOT);
        if (normalized.length() != 2 || !ISO_639_1_LANGUAGES.contains(normalized)) {
            throw new BallotValidationException("language must be a valid ISO 639-1 code");
        }
        return normalized;
    }
}
