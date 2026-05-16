package io.mirems.core.bpmn.audit;

import java.util.List;

public record StatisticalSample(long seed, int sampleSize, List<String> sampledBallotIds) {
    public StatisticalSample {
        sampledBallotIds = List.copyOf(sampledBallotIds);
    }
}
