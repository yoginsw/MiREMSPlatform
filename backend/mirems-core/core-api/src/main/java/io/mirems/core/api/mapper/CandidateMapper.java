package io.mirems.core.api.mapper;

import io.mirems.core.api.dto.CandidateResponse;
import io.mirems.core.domain.contest.Candidate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CandidateMapper {
    @Mapping(target = "contestId", source = "contest.id")
    @Mapping(target = "electionId", source = "contest.election.id")
    CandidateResponse toResponse(Candidate candidate);
}
