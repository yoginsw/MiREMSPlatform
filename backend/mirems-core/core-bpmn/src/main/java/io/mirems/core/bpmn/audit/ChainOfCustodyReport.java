package io.mirems.core.bpmn.audit;

import java.util.List;

public record ChainOfCustodyReport(List<ChainOfCustodyEntry> entries) {
    public ChainOfCustodyReport {
        entries = List.copyOf(entries);
    }
}
