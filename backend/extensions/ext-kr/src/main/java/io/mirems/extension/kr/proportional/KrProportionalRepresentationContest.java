package io.mirems.extension.kr.proportional;

import io.mirems.extension.kr.KrElectionType;

public final class KrProportionalRepresentationContest {
    public static final String CONTEST_TYPE_CODE = "PROPORTIONAL_REPRESENTATION";
    public static final String KOREAN_LABEL = "비례대표";
    private static final String TITLE = "비례대표 국회의원선거";
    private static final int VOTE_LIMIT = 1;

    private final String code;
    private final String title;
    private final int seats;

    private KrProportionalRepresentationContest(int seats) {
        if (seats < 1) {
            throw new IllegalArgumentException("proportional representation seats must be greater than zero");
        }
        this.code = CONTEST_TYPE_CODE;
        this.title = TITLE;
        this.seats = seats;
    }

    public static KrProportionalRepresentationContest create(int seats) {
        return new KrProportionalRepresentationContest(seats);
    }

    public static KrElectionType supportedElectionType() {
        return KrElectionType.NATIONAL_ASSEMBLY_ELECTION;
    }

    public static int voteLimit() {
        return VOTE_LIMIT;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public int seats() {
        return seats;
    }

    public int voteLimitValue() {
        return VOTE_LIMIT;
    }
}
