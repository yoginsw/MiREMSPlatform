package io.mirems.core.api.mapper;

import io.mirems.core.api.dto.ElectionResponse;
import io.mirems.core.domain.election.Election;
import org.mapstruct.Mapper;

@Mapper
public interface ElectionMapper {
    ElectionResponse toResponse(Election election);
}
