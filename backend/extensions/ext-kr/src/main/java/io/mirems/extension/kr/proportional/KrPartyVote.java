package io.mirems.extension.kr.proportional;

public record KrPartyVote(String partyId, String partyName, int votes) {
    public KrPartyVote {
        partyId = requireText(partyId, "partyId");
        partyName = requireText(partyName, "partyName");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
