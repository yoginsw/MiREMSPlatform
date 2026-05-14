package io.mirems.core.bpmn.correction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mirems.core.domain.ballot.AccessibilityFeature;
import io.mirems.core.domain.ballot.Ballot;
import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.result.CorrectionStatus;
import io.mirems.core.domain.result.VoteCorrectedEvent;
import io.mirems.core.domain.result.VoteCorrection;
import io.mirems.core.domain.result.VoteCorrectionRepository;
import io.mirems.core.domain.result.VotingResult;
import io.mirems.core.domain.result.VotingResultRepository;
import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.VotingSession;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VoteCorrectionProcessServiceTest {
    private static final UUID RESULT_ID = UUID.fromString("018f4bd0-1111-7222-8333-944455556666");
    private static final UUID CORRECTION_ID = UUID.fromString("018f4bd0-2222-7333-8444-a55566667777");
    private static final UUID CANDIDATE_ONE = UUID.fromString("018f4bd0-3333-7444-8555-b66677778888");
    private static final UUID CANDIDATE_TWO = UUID.fromString("018f4bd0-4444-7555-8666-c77788889999");
    private static final OffsetDateTime CAST_AT = OffsetDateTime.parse("2026-06-03T09:30:00Z");

    @Test
    void completesDualApprovalCorrectionWithoutMutatingOriginalVotingResult() {
        VotingResult original = originalResult();
        original.computeHashBeforePersist();
        String originalHash = original.getHash();
        InMemoryVotingResultRepository votingResults = new InMemoryVotingResultRepository(original);
        InMemoryVoteCorrectionRepository corrections = new InMemoryVoteCorrectionRepository();
        VoteCorrectionProcessService service = new VoteCorrectionProcessService(votingResults, corrections);

        VoteCorrectionProcessResult result = service.correctVote(new VoteCorrectionRequest(
                CORRECTION_ID,
                RESULT_ID,
                List.of(CANDIDATE_TWO),
                "Administrative data entry correction",
                "operator-001",
                CAST_AT.plusHours(1),
                "admin-a",
                CAST_AT.plusHours(2),
                "admin-b",
                CAST_AT.plusHours(3)));

        assertEquals(CorrectionStatus.APPROVED, result.correction().getCorrectionStatus());
        assertEquals(List.of(CANDIDATE_TWO), result.correction().getCorrectedCandidateIds());
        assertEquals(RESULT_ID, result.event().originalVotingResultId());
        assertEquals(List.of(CANDIDATE_ONE), result.event().originalCandidateIds());
        assertEquals(List.of(CANDIDATE_TWO), result.event().correctedCandidateIds());
        assertEquals("admin-a", result.event().firstApprovedBy());
        assertEquals("admin-b", result.event().secondApprovedBy());
        assertEquals(1, corrections.savedCorrections.size());
        assertEquals(List.of(CANDIDATE_ONE), original.getSelectedCandidateIds());
        assertEquals(originalHash, original.getHash());
        assertEquals(0, votingResults.saveCalls);
    }

    @Test
    void correctionFailsWhenDualApproverIsSameElectionAdmin() {
        VoteCorrectionProcessService service = new VoteCorrectionProcessService(
                new InMemoryVotingResultRepository(originalResult()),
                new InMemoryVoteCorrectionRepository());

        VoteCorrectionRequest request = new VoteCorrectionRequest(
                CORRECTION_ID,
                RESULT_ID,
                List.of(CANDIDATE_TWO),
                "Administrative data entry correction",
                "operator-001",
                CAST_AT.plusHours(1),
                "admin-a",
                CAST_AT.plusHours(2),
                "admin-a",
                CAST_AT.plusHours(3));

        assertThrows(IllegalArgumentException.class, () -> service.correctVote(request));
    }

    private static final class InMemoryVotingResultRepository implements VotingResultRepository {
        private final VotingResult original;
        private int saveCalls;

        private InMemoryVotingResultRepository(VotingResult original) {
            this.original = original;
        }

        @Override
        public VotingResult save(VotingResult votingResult) {
            saveCalls++;
            return votingResult;
        }

        @Override
        public Optional<VotingResult> findById(UUID id) {
            return RESULT_ID.equals(id) ? Optional.of(original) : Optional.empty();
        }

        @Override
        public List<VotingResult> findBySessionId(UUID sessionId) {
            return List.of();
        }

        @Override
        public List<VotingResult> findByContestId(UUID contestId) {
            return List.of();
        }
    }

    private static final class InMemoryVoteCorrectionRepository implements VoteCorrectionRepository {
        private final List<VoteCorrection> savedCorrections = new ArrayList<>();

        @Override
        public VoteCorrection save(VoteCorrection correction) {
            savedCorrections.add(correction);
            return correction;
        }

        @Override
        public Optional<VoteCorrection> findById(UUID id) {
            return savedCorrections.stream().filter(correction -> correction.getId().equals(id)).findFirst();
        }

        @Override
        public List<VoteCorrection> findByOriginalVotingResultId(UUID originalVotingResultId) {
            return savedCorrections.stream()
                    .filter(correction -> correction.getOriginalVotingResult().getId().equals(originalVotingResultId))
                    .toList();
        }
    }

    private static VotingResult originalResult() {
        return VotingResult.create(RESULT_ID, votingSession(), contest(), List.of(CANDIDATE_ONE), CAST_AT);
    }

    private static VotingSession votingSession() {
        return voterRecord().openVotingSession(
                UUID.fromString("018f4bd0-5555-7666-8777-d88899990000"),
                election(),
                ballotStyle(),
                "DEVICE-001",
                CAST_AT.minusMinutes(10));
    }

    private static VoterRecord voterRecord() {
        return VoterRecord.create(
                UUID.fromString("018f4bd0-6666-7777-8888-e99900001111"),
                "KR-SEOUL-000001",
                Set.of(election().getId()),
                RegistrationStatus.ACTIVE,
                encryptionService());
    }

    private static PiiEncryptionService encryptionService() {
        byte[] key = Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        return new PiiEncryptionService(key);
    }

    private static BallotStyle ballotStyle() {
        Ballot ballot = Ballot.create(UUID.fromString("018f4bd0-7777-7888-8999-f00011112222"), election());
        ballot.addContest(contest(), 1, "Mayor selection");
        return ballot.addStyle(
                UUID.fromString("018f4bd0-8888-7999-8aaa-011122223333"),
                "SEOUL-01-KO",
                "Seoul District 1",
                "ko",
                Set.of(AccessibilityFeature.LARGE_TEXT));
    }

    private static Contest contest() {
        return Contest.create(
                UUID.fromString("018f4bd0-9999-7aaa-8bbb-122233334444"),
                election(),
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                1,
                1);
    }

    private static Election election() {
        Election election = Election.create(
                UUID.fromString("018f4bd0-aaaa-7bbb-8ccc-233344445555"),
                "2026 Local Election",
                ElectionType.LOCAL,
                "Seoul",
                LocalDate.of(2026, 6, 3),
                "KR",
                "ext-kr");
        election.pullDomainEvents();
        return election;
    }
}
