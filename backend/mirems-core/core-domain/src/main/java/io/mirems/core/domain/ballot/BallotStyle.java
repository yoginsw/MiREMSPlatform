package io.mirems.core.domain.ballot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Entity representing a jurisdiction/language/accessibility-specific ballot presentation. */
@Entity
@Table(name = "ballot_styles")
public class BallotStyle {
    private static final Set<String> ISO_639_1_LANGUAGES = Set.of(Locale.getISOLanguages());

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ballot_id", nullable = false)
    private Ballot ballot;

    @Column(name = "style_code", nullable = false, unique = true)
    private String styleCode;

    @Column(name = "district", nullable = false)
    private String district;

    @Column(name = "language", nullable = false, length = 2)
    private String language;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "accessibility_features", nullable = false, columnDefinition = "jsonb")
    private Set<AccessibilityFeature> accessibilityFeatures;

    protected BallotStyle() {
        // JPA constructor.
    }

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
