package io.mirems.extension.kr.proportional;

import java.util.List;

public record KrPartyListBallotLayout(
        String contestType,
        String title,
        String instructions,
        String type,
        int maxSelections,
        List<Option> options) {

    public KrPartyListBallotLayout {
        options = List.copyOf(options);
    }

    public record Option(String id, String label, String partyLogoUri) {}
}
