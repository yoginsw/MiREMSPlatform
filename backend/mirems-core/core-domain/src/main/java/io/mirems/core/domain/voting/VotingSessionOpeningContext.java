package io.mirems.core.domain.voting;

import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.election.Election;
import java.time.OffsetDateTime;

public record VotingSessionOpeningContext(
        VoterRecord voterRecord,
        Election election,
        BallotStyle ballotStyle,
        VotingMethod votingMethod,
        OffsetDateTime openedAt,
        String homeDistrictCode,
        String pollingStationDistrictCode) {}
