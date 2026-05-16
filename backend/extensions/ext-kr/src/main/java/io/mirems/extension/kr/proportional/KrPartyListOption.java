package io.mirems.extension.kr.proportional;

public record KrPartyListOption(String partyId, String partyName, String partyLogoUri, int listOrder) {
    public KrPartyListOption {
        partyId = requireText(partyId, "partyId");
        partyName = requireText(partyName, "partyName");
        partyLogoUri = partyLogoUri == null ? "" : partyLogoUri.strip();
        if (listOrder < 1) {
            throw new IllegalArgumentException("party list order must be greater than zero");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
