package com.sms.dto;

import java.util.List;
import java.util.Map;

public class WorkloadBalanceResponse {

    private Map<String, Integer> workload;
    private int maxAllowed;
    private List<String> overloadedFaculties;
    private int rebalancedEntries;

    public Map<String, Integer> getWorkload() {
        return workload;
    }

    public void setWorkload(Map<String, Integer> workload) {
        this.workload = workload;
    }

    public int getMaxAllowed() {
        return maxAllowed;
    }

    public void setMaxAllowed(int maxAllowed) {
        this.maxAllowed = maxAllowed;
    }

    public List<String> getOverloadedFaculties() {
        return overloadedFaculties;
    }

    public void setOverloadedFaculties(List<String> overloadedFaculties) {
        this.overloadedFaculties = overloadedFaculties;
    }

    public int getRebalancedEntries() {
        return rebalancedEntries;
    }

    public void setRebalancedEntries(int rebalancedEntries) {
        this.rebalancedEntries = rebalancedEntries;
    }
}
