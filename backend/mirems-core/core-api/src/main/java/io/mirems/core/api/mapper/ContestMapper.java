package io.mirems.core.api.mapper;

import io.mirems.core.api.dto.ContestResponse;
import io.mirems.core.domain.contest.Contest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ContestMapper {
    @Mapping(target = "electionId", source = "election.id")
    ContestResponse toResponse(Contest contest);
}
