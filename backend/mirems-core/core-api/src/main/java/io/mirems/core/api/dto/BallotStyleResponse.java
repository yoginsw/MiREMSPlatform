package io.mirems.core.api.dto;

import io.mirems.core.domain.ballot.AccessibilityFeature;
import java.util.Set;
import java.util.UUID;

public record BallotStyleResponse(
        UUID id,
        UUID ballotId,
        String styleCode,
        String district,
        String language,
        Set<AccessibilityFeature> accessibilityFeatures) {
}
