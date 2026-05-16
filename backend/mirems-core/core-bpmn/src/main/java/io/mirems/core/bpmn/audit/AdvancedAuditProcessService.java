package io.mirems.core.bpmn.audit;

import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({AuditEventRepository.class, AdvancedAuditAuthorizer.class})
public class AdvancedAuditProcessService {
    private static final List<String> REQUIRED_VVSG_EVENT_TYPES = List.of(
            "ElectionCreated",
            "VotingSessionOpened",
            "VoteCast",
            "TabulationCompleted",
            "ResultCertified");

    private final AuditEventRepository auditEventRepository;
    private final AdvancedAuditAuthorizer authorizer;

    public AdvancedAuditProcessService(AuditEventRepository auditEventRepository, AdvancedAuditAuthorizer authorizer) {
        this.auditEventRepository = Objects.requireNonNull(auditEventRepository, "auditEventRepository is required");
        this.authorizer = Objects.requireNonNull(authorizer, "authorizer is required");
    }

    public AdvancedAuditReport generate(AdvancedAuditRequest request) {
        Objects.requireNonNull(request, "request is required");
        authorizer.requireAuditor(request.initiatedBy(), request.electionId());
        List<AuditEvent> events = auditEventRepository.findByAggregateId(request.electionId()).stream()
                .sorted(Comparator.comparing(AuditEvent::getOccurredAt).thenComparing(AuditEvent::getId))
                .toList();
        RiskLimitingAuditPlan rlaPlan = buildRiskLimitingAuditPlan(request);
        StatisticalSample sample = buildSample(request, rlaPlan.recommendedSampleSize());
        ChainOfCustodyReport chainOfCustodyReport = buildChainOfCustody(events);
        VvsgAuditTrailVerification vvsgVerification = verifyCompleteness(events);
        ExternalAuditExport jsonExport = jsonExport(request, rlaPlan, sample, chainOfCustodyReport, vvsgVerification);
        ExternalAuditExport xmlExport = xmlExport(request, rlaPlan, sample, chainOfCustodyReport, vvsgVerification);
        return new AdvancedAuditReport(request, rlaPlan, sample, chainOfCustodyReport, vvsgVerification, jsonExport, xmlExport);
    }

    private static RiskLimitingAuditPlan buildRiskLimitingAuditPlan(AdvancedAuditRequest request) {
        int baseSample = (int) Math.ceil(Math.log(request.riskLimit()) / Math.log(1.0 - request.margin()));
        int recommendedSampleSize = Math.max(1, (int) Math.ceil(baseSample / 10.0) * 100);
        return new RiskLimitingAuditPlan(
                request.ballotCount(),
                request.reportedMarginVotes(),
                request.margin(),
                request.riskLimit(),
                recommendedSampleSize);
    }

    private static StatisticalSample buildSample(AdvancedAuditRequest request, int recommendedSampleSize) {
        List<String> population = request.ballotPopulation();
        int sampleSize = Math.min(population.size(), recommendedSampleSize);
        java.util.ArrayList<String> shuffledPopulation = new java.util.ArrayList<>(population);
        java.util.Collections.shuffle(shuffledPopulation, new java.util.Random(request.randomSeed()));
        List<String> sampledBallotIds = shuffledPopulation.subList(0, sampleSize).stream().sorted().toList();
        return new StatisticalSample(request.randomSeed(), sampleSize, sampledBallotIds);
    }

    private static ChainOfCustodyReport buildChainOfCustody(List<AuditEvent> events) {
        return new ChainOfCustodyReport(events.stream()
                .map(event -> new ChainOfCustodyEntry(
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getActorId(),
                        event.getOccurredAt(),
                        sanitizedPayloadKeys(event)))
                .toList());
    }

    private static VvsgAuditTrailVerification verifyCompleteness(List<AuditEvent> events) {
        Set<String> present = events.stream().map(AuditEvent::getEventType).collect(java.util.stream.Collectors.toSet());
        List<String> missing = REQUIRED_VVSG_EVENT_TYPES.stream()
                .filter(required -> !present.contains(required))
                .toList();
        return new VvsgAuditTrailVerification(missing.isEmpty(), missing);
    }

    private static Set<String> sanitizedPayloadKeys(AuditEvent event) {
        return event.getPayload().keySet().stream()
                .filter(key -> !isSensitivePayloadKey(key))
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
    }

    private static boolean isSensitivePayloadKey(String key) {
        String normalized = key.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("name")
                || normalized.contains("address")
                || normalized.contains("ssn")
                || normalized.contains("email")
                || normalized.contains("phone")
                || normalized.contains("dob")
                || normalized.contains("birth")
                || normalized.contains("voterid")
                || normalized.contains("voter_id")
                || normalized.contains("ipaddress")
                || normalized.contains("ip_address")
                || normalized.contains("token")
                || normalized.contains("secret");
    }

    private static ExternalAuditExport jsonExport(
            AdvancedAuditRequest request,
            RiskLimitingAuditPlan plan,
            StatisticalSample sample,
            ChainOfCustodyReport chain,
            VvsgAuditTrailVerification vvsg) {
        StringBuilder builder = new StringBuilder();
        builder.append('{')
                .append("\"reportId\":\"").append(request.reportId()).append("\",")
                .append("\"electionId\":\"").append(request.electionId()).append("\",")
                .append("\"generatedBy\":\"").append(escape(request.initiatedBy())).append("\",")
                .append("\"riskLimit\":").append(plan.riskLimit()).append(',')
                .append("\"margin\":").append(plan.margin()).append(',')
                .append("\"recommendedSampleSize\":").append(plan.recommendedSampleSize()).append(',')
                .append("\"sampledBallotIds\":[").append(quotedList(sample.sampledBallotIds())).append("],")
                .append("\"vvsgComplete\":").append(vvsg.complete()).append(',')
                .append("\"missingEventTypes\":[").append(quotedList(vvsg.missingEventTypes())).append("],")
                .append("\"chainOfCustody\":[");
        for (int index = 0; index < chain.entries().size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            ChainOfCustodyEntry entry = chain.entries().get(index);
            builder.append('{')
                    .append("\"eventId\":\"").append(entry.eventId()).append("\",")
                    .append("\"eventType\":\"").append(escape(entry.eventType())).append("\",")
                    .append("\"actorId\":\"").append(escape(entry.actorId())).append("\",")
                    .append("\"occurredAt\":\"").append(entry.occurredAt()).append("\",")
                    .append("\"payloadKeys\":[").append(quotedList(entry.payloadKeys().stream().sorted().toList())).append(']')
                    .append('}');
        }
        builder.append("]}");
        return new ExternalAuditExport(
                "advanced-audit-" + request.electionId() + ".json",
                "application/vnd.mirems.audit+json",
                builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static ExternalAuditExport xmlExport(
            AdvancedAuditRequest request,
            RiskLimitingAuditPlan plan,
            StatisticalSample sample,
            ChainOfCustodyReport chain,
            VvsgAuditTrailVerification vvsg) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<advancedAuditReport>")
                .append("<reportId>").append(request.reportId()).append("</reportId>")
                .append("<electionId>").append(request.electionId()).append("</electionId>")
                .append("<riskLimit>").append(plan.riskLimit()).append("</riskLimit>")
                .append("<recommendedSampleSize>").append(plan.recommendedSampleSize()).append("</recommendedSampleSize>")
                .append("<sampledBallots>");
        for (String ballotId : sample.sampledBallotIds()) {
            builder.append("<ballotId>").append(escapeXml(ballotId)).append("</ballotId>");
        }
        builder.append("</sampledBallots>")
                .append("<vvsgComplete>").append(vvsg.complete()).append("</vvsgComplete>")
                .append("<missingEventTypes>");
        for (String missingEventType : vvsg.missingEventTypes()) {
            builder.append("<eventType>").append(escapeXml(missingEventType)).append("</eventType>");
        }
        builder.append("</missingEventTypes>")
                .append("<chainOfCustody>");
        for (ChainOfCustodyEntry entry : chain.entries()) {
            builder.append("<entry>")
                    .append("<eventId>").append(entry.eventId()).append("</eventId>")
                    .append("<eventType>").append(escapeXml(entry.eventType())).append("</eventType>")
                    .append("<actorId>").append(escapeXml(entry.actorId())).append("</actorId>")
                    .append("<occurredAt>").append(entry.occurredAt()).append("</occurredAt>")
                    .append("</entry>");
        }
        builder.append("</chainOfCustody></advancedAuditReport>");
        return new ExternalAuditExport(
                "advanced-audit-" + request.electionId() + ".xml",
                "application/vnd.mirems.audit+xml",
                builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String quotedList(List<String> values) {
        return values.stream().map(value -> "\"" + escape(value) + "\"").collect(java.util.stream.Collectors.joining(","));
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
