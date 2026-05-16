package io.mirems.extension.kr;

import io.mirems.core.domain.election.ElectionType;

public enum KrElectionType {
    PRESIDENTIAL_ELECTION(ElectionType.PRESIDENTIAL, "대통령선거", "presidential-election"),
    NATIONAL_ASSEMBLY_ELECTION(ElectionType.PARLIAMENTARY, "국회의원선거", "national-assembly-election"),
    LOCAL_ELECTION(ElectionType.LOCAL, "지방선거", "local-election"),
    SUPERINTENDENT_ELECTION(ElectionType.LOCAL, "교육감선거", "superintendent-election"),
    BY_ELECTION(ElectionType.REGIONAL, "보궐선거", "by-election");

    private final ElectionType coreElectionType;
    private final String koreanLabel;
    private final String slug;

    KrElectionType(ElectionType coreElectionType, String koreanLabel, String slug) {
        this.coreElectionType = coreElectionType;
        this.koreanLabel = koreanLabel;
        this.slug = slug;
    }

    public ElectionType coreElectionType() {
        return coreElectionType;
    }

    public String koreanLabel() {
        return koreanLabel;
    }

    public String slug() {
        return slug;
    }
}
