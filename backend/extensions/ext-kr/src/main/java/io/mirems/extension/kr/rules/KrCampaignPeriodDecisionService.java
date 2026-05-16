package io.mirems.extension.kr.rules;

import io.mirems.extension.kr.KrElectionType;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class KrCampaignPeriodDecisionService {
    public static final String DECISION_ID = "KrCampaignPeriod";
    public static final String DMN_RESOURCE = "decisions/kr/KrCampaignPeriod.dmn";

    public KrCampaignPeriodResult evaluate(KrCampaignPeriodRequest request) {
        KrCampaignPeriodRequest input = Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(input.electionType(), "electionType is required");

        if (input.daysUntilElection() <= 0) {
            return new KrCampaignPeriodResult(false, "campaign activity is prohibited on or after election day");
        }
        if (input.electionType() == KrElectionType.PRESIDENTIAL_ELECTION) {
            if (input.daysUntilElection() <= 23) {
                return new KrCampaignPeriodResult(true, "presidential campaign period is active");
            }
            return new KrCampaignPeriodResult(false, "presidential campaign period has not started");
        }
        if (input.daysUntilElection() <= 14) {
            return new KrCampaignPeriodResult(true, "standard public election campaign period is active");
        }
        return new KrCampaignPeriodResult(false, "standard public election campaign period has not started");
    }
}
