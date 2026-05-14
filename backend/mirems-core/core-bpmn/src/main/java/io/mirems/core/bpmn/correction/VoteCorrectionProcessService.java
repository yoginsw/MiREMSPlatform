package io.mirems.core.bpmn.correction;

import io.mirems.core.domain.result.VoteCorrectedEvent;
import io.mirems.core.domain.result.VoteCorrection;
import io.mirems.core.domain.result.VoteCorrectionRepository;
import io.mirems.core.domain.result.VotingResult;
import io.mirems.core.domain.result.VotingResultRepository;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({VotingResultRepository.class, VoteCorrectionRepository.class})
public class VoteCorrectionProcessService {
    private final VotingResultRepository votingResultRepository;
    private final VoteCorrectionRepository voteCorrectionRepository;

    public VoteCorrectionProcessService(
            VotingResultRepository votingResultRepository,
            VoteCorrectionRepository voteCorrectionRepository) {
        this.votingResultRepository = Objects.requireNonNull(votingResultRepository, "votingResultRepository is required");
        this.voteCorrectionRepository = Objects.requireNonNull(voteCorrectionRepository, "voteCorrectionRepository is required");
    }

    public VoteCorrectionProcessResult correctVote(VoteCorrectionRequest request) {
        Objects.requireNonNull(request, "request is required");
        VotingResult original = votingResultRepository
                .findById(request.originalVotingResultId())
                .orElseThrow(() -> new IllegalArgumentException("original voting result not found: "
                        + request.originalVotingResultId()));
        VoteCorrection correction = VoteCorrection.request(
                request.correctionId(),
                original,
                request.correctedCandidateIds(),
                request.reason(),
                request.requestedBy(),
                request.requestedAt());
        correction.recordFirstApproval(request.firstApprover(), request.firstApprovedAt());
        VoteCorrectedEvent event = correction.recordSecondApproval(request.secondApprover(), request.secondApprovedAt());
        voteCorrectionRepository.save(correction);
        return new VoteCorrectionProcessResult(correction, event);
    }
}
