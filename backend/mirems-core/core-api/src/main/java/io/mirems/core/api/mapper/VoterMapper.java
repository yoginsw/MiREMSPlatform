package io.mirems.core.api.mapper;

import io.mirems.core.api.dto.VoterResponse;
import io.mirems.core.domain.voting.VoterRecord;
import org.mapstruct.Mapper;

@Mapper
public interface VoterMapper {
    VoterResponse toResponse(VoterRecord voterRecord);
}
