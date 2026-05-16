package io.mirems.extension.kr.rules;

import io.mirems.extension.kr.KrElectionType;

public record KrCampaignPeriodRequest(KrElectionType electionType, int daysUntilElection) {}
