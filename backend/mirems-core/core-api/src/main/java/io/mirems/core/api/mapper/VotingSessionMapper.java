package io.mirems.core.api.mapper;

import io.mirems.core.api.dto.VotingSessionResponse;
import io.mirems.core.domain.voting.VotingSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface VotingSessionMapper {
    @Mapping(target = "voterRecordId", source = "voterRecord.id")
    @Mapping(target = "electionId", source = "election.id")
    @Mapping(target = "ballotStyleId", source = "ballotStyle.id")
    VotingSessionResponse toResponse(VotingSession votingSession);
}
