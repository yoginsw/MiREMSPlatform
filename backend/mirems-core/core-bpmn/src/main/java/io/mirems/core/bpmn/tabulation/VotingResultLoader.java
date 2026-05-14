package io.mirems.core.bpmn.tabulation;

import io.mirems.core.domain.result.VotingResult;
import java.util.List;
import java.util.UUID;

/** Loads immutable voting results that are eligible for tabulation. */
public interface VotingResultLoader {
    List<VotingResult> loadCommittedResults(UUID electionId);
}
