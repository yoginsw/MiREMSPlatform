package io.mirems.core.domain.ballot;

import io.mirems.core.domain.contest.Contest;
import java.util.Objects;

/** Join entity connecting a ballot to a contest with presentation ordering. */
public class BallotContest {
    private final Ballot ballot;
    private final Contest contest;
    private final int displayOrder;
    private final String presentationTitle;

    private BallotContest(Ballot ballot, Contest contest, int displayOrder, String presentationTitle) {
        this.ballot = Objects.requireNonNull(ballot, "ballot is required");
        this.contest = Objects.requireNonNull(contest, "contest is required");
        if (displayOrder < 1) {
            throw new BallotValidationException("displayOrder must be greater than zero");
        }
        this.displayOrder = displayOrder;
        this.presentationTitle = Ballot.requireText(presentationTitle, "presentationTitle");
    }

    static BallotContest create(Ballot ballot, Contest contest, int displayOrder, String presentationTitle) {
        return new BallotContest(ballot, contest, displayOrder, presentationTitle);
    }

    public Ballot getBallot() {
        return ballot;
    }

    public Contest getContest() {
        return contest;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getPresentationTitle() {
        return presentationTitle;
    }
}
