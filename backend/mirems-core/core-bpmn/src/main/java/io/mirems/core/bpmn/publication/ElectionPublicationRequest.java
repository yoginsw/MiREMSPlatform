package io.mirems.core.bpmn.publication;

import io.mirems.core.domain.election.Election;

public record ElectionPublicationRequest(
        Election election,
        String reviewerRole,
        boolean requiredContestsDefined,
        boolean ballotStylesCoverAllDistricts) {
}
