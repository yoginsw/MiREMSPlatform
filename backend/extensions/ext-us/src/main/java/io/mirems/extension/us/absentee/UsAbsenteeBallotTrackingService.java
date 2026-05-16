package io.mirems.extension.us.absentee;

import io.mirems.extension.us.rules.UsAbsenteeBallotDecisionService;
import io.mirems.extension.us.rules.UsAbsenteeBallotRequest;
import io.mirems.extension.us.rules.UsAbsenteeVoterCategory;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UsAbsenteeBallotTrackingService {
    private final UsAbsenteeBallotDecisionService decisionService;

    public UsAbsenteeBallotTrackingService() {
        this(new UsAbsenteeBallotDecisionService());
    }

    public UsAbsenteeBallotTrackingService(UsAbsenteeBallotDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    public UsAbsenteeBallotRecord requestBallot(
            UUID voterId,
            UUID electionId,
            UsAbsenteeBallotRequest request,
            OffsetDateTime requestedAt) {
        var decision = decisionService.evaluate(request);
        if (!decision.eligible() && !decision.federalWriteInAbsenteeBallotAllowed()) {
            throw new IllegalArgumentException("not absentee eligible: " + decision.reason());
        }
        boolean uocava = isUocava(request.voterCategory());
        boolean fwab = decision.federalWriteInAbsenteeBallotAllowed();
        UsAbsenteeBallotStatus status = fwab ? UsAbsenteeBallotStatus.FWAB_ALLOWED : UsAbsenteeBallotStatus.REQUESTED;
        UUID id = UUID.nameUUIDFromBytes((voterId + ":" + electionId + ":" + request.voterCategory() + ":" + requestedAt).getBytes(StandardCharsets.UTF_8));
        return new UsAbsenteeBallotRecord(
                id,
                voterId,
                electionId,
                request.voterCategory(),
                status,
                uocava,
                fwab,
                requestedAt,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of("REQUESTED:" + request.voterCategory()));
    }

    public UsAbsenteeBallotRecord markSent(UsAbsenteeBallotRecord record, OffsetDateTime sentAt, String deliveryMethod) {
        if (record.status() == UsAbsenteeBallotStatus.FWAB_ALLOWED) {
            throw new IllegalStateException("FWAB fallback records cannot be sent");
        }
        requireStatus(record, UsAbsenteeBallotStatus.REQUESTED, "must be REQUESTED before SENT");
        String method = java.util.Objects.requireNonNull(deliveryMethod, "deliveryMethod is required").trim();
        if (method.isEmpty()) {
            throw new IllegalArgumentException("deliveryMethod is required");
        }
        List<String> audit = append(record.auditTrail(), "SENT:" + method);
        return copy(record, UsAbsenteeBallotStatus.SENT, Optional.of(sentAt), Optional.of(method), record.returnedAt(), record.adjudicatedAt(), audit);
    }

    public UsAbsenteeBallotRecord markReturned(UsAbsenteeBallotRecord record, OffsetDateTime returnedAt) {
        requireStatus(record, UsAbsenteeBallotStatus.SENT, "must be SENT before RETURNED");
        return copy(record, UsAbsenteeBallotStatus.RETURNED, record.sentAt(), record.deliveryMethod(), Optional.of(returnedAt), record.adjudicatedAt(), append(record.auditTrail(), "RETURNED"));
    }

    public UsAbsenteeBallotRecord adjudicate(UsAbsenteeBallotRecord record, boolean accepted, String reason, OffsetDateTime adjudicatedAt) {
        requireStatus(record, UsAbsenteeBallotStatus.RETURNED, "must be RETURNED before adjudication");
        UsAbsenteeBallotStatus status = accepted ? UsAbsenteeBallotStatus.ACCEPTED : UsAbsenteeBallotStatus.REJECTED;
        return copy(record, status, record.sentAt(), record.deliveryMethod(), record.returnedAt(), Optional.of(adjudicatedAt), append(record.auditTrail(), "ADJUDICATED:" + status));
    }

    private static UsAbsenteeBallotRecord copy(
            UsAbsenteeBallotRecord record,
            UsAbsenteeBallotStatus status,
            Optional<OffsetDateTime> sentAt,
            Optional<String> deliveryMethod,
            Optional<OffsetDateTime> returnedAt,
            Optional<OffsetDateTime> adjudicatedAt,
            List<String> auditTrail) {
        return new UsAbsenteeBallotRecord(record.id(), record.voterId(), record.electionId(), record.voterCategory(), status, record.uocava(), record.federalWriteInAbsenteeBallotAllowed(), record.requestedAt(), sentAt, deliveryMethod, returnedAt, adjudicatedAt, auditTrail);
    }

    private static void requireStatus(UsAbsenteeBallotRecord record, UsAbsenteeBallotStatus expected, String message) {
        if (record.status() != expected) {
            throw new IllegalStateException(message);
        }
    }

    private static List<String> append(List<String> existing, String item) {
        List<String> audit = new ArrayList<>(existing);
        audit.add(item);
        return List.copyOf(audit);
    }

    private static boolean isUocava(UsAbsenteeVoterCategory category) {
        return category == UsAbsenteeVoterCategory.MILITARY || category == UsAbsenteeVoterCategory.OVERSEAS_CITIZEN;
    }
}
