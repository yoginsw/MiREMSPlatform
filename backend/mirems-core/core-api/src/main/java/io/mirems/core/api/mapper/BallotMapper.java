package io.mirems.core.api.mapper;

import io.mirems.core.api.dto.BallotContestResponse;
import io.mirems.core.api.dto.BallotResponse;
import io.mirems.core.api.dto.BallotStyleResponse;
import io.mirems.core.domain.ballot.Ballot;
import io.mirems.core.domain.ballot.BallotContest;
import io.mirems.core.domain.ballot.BallotStyle;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BallotMapper {
    @Mapping(target = "electionId", source = "election.id")
    @Mapping(target = "contests", expression = "java(toContestResponses(ballot.getBallotContests()))")
    @Mapping(target = "styles", expression = "java(toStyleResponses(ballot.getBallotStyles()))")
    BallotResponse toResponse(Ballot ballot);

    @Mapping(target = "contestId", source = "contest.id")
    BallotContestResponse toResponse(BallotContest ballotContest);

    @Mapping(target = "ballotId", source = "ballot.id")
    BallotStyleResponse toResponse(BallotStyle ballotStyle);

    default List<BallotContestResponse> toContestResponses(List<BallotContest> contests) {
        return contests.stream().map(this::toResponse).toList();
    }

    default List<BallotStyleResponse> toStyleResponses(List<BallotStyle> styles) {
        return styles.stream().map(this::toResponse).toList();
    }
}
