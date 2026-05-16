package io.mirems.extension.kr.proportional;

import java.util.List;

public record KrDhondtSeatAllocationResult(List<KrPartySeatAllocation> allocations, int totalSeatsAllocated, String reason) {
    public KrDhondtSeatAllocationResult {
        allocations = List.copyOf(allocations);
    }
}
