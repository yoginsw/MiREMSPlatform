package io.mirems.extension.us.rules;

public record UsVoterEligibilityResult(boolean eligible, boolean provisionalBallotRequired, String reason) {}
