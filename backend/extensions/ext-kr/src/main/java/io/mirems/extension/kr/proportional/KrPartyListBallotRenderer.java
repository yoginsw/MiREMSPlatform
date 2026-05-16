package io.mirems.extension.kr.proportional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class KrPartyListBallotRenderer {
    private static final String INSTRUCTIONS = "정당명부 중 하나의 정당만 선택해 주세요.";

    public KrPartyListBallotLayout render(
            KrProportionalRepresentationContest contest,
            List<KrPartyListOption> partyList) {
        Objects.requireNonNull(contest, "contest is required");
        Objects.requireNonNull(partyList, "partyList is required");
        if (partyList.isEmpty()) {
            throw new IllegalArgumentException("party list must not be empty");
        }
        validateUniquePartyIds(partyList);
        List<KrPartyListBallotLayout.Option> options = partyList.stream()
                .sorted(Comparator.comparingInt(KrPartyListOption::listOrder).thenComparing(KrPartyListOption::partyId))
                .map(option -> new KrPartyListBallotLayout.Option(
                        option.partyId(),
                        option.partyName(),
                        option.partyLogoUri()))
                .toList();
        return new KrPartyListBallotLayout(
                contest.code(),
                contest.title(),
                INSTRUCTIONS,
                "single",
                contest.voteLimitValue(),
                options);
    }

    private static void validateUniquePartyIds(List<KrPartyListOption> partyList) {
        Set<String> seen = new HashSet<>();
        for (KrPartyListOption option : partyList) {
            if (!seen.add(option.partyId())) {
                throw new IllegalArgumentException("party list contains duplicate party id: " + option.partyId());
            }
        }
    }
}
