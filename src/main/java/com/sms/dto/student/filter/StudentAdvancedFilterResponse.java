package com.sms.dto.student.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentAdvancedFilterResponse {

    private List<Map<String, Object>> items = new ArrayList<>();
    private List<String> appliedFilters = new ArrayList<>();
    private List<String> smartSuggestions = new ArrayList<>();
    private Integer page = 0;
    private Integer size = 50;
    private Long totalElements = 0L;
    private Integer totalPages = 0;
    private Boolean hasNext = false;
    private Boolean hasPrevious = false;
    private String interpretedSmartQuery;

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, Object>> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public List<String> getAppliedFilters() {
        return appliedFilters;
    }

    public void setAppliedFilters(List<String> appliedFilters) {
        this.appliedFilters = appliedFilters == null ? new ArrayList<>() : appliedFilters;
    }

    public List<String> getSmartSuggestions() {
        return smartSuggestions;
    }

    public void setSmartSuggestions(List<String> smartSuggestions) {
        this.smartSuggestions = smartSuggestions == null ? new ArrayList<>() : smartSuggestions;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Boolean getHasNext() {
        return hasNext;
    }

    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    public Boolean getHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public String getInterpretedSmartQuery() {
        return interpretedSmartQuery;
    }

    public void setInterpretedSmartQuery(String interpretedSmartQuery) {
        this.interpretedSmartQuery = interpretedSmartQuery;
    }
}
