package io.mirems.core.api.mapper;

import io.mirems.core.api.dto.VotingResultResponse;
import io.mirems.core.domain.result.VotingResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface VotingResultMapper {
    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "contestId", source = "contest.id")
    VotingResultResponse toResponse(VotingResult votingResult);
}
