package com.sms.dto.student.filter;

public class StudentAdvancedFilterRequest {

    private StudentFilterNode filterGroup;
    private String smartQuery;
    private Integer page = 0;
    private Integer size = 50;
    private String sortBy = "id";
    private String sortDir = "asc";
    private Boolean includeSensitive = false;

    public StudentFilterNode getFilterGroup() {
        return filterGroup;
    }

    public void setFilterGroup(StudentFilterNode filterGroup) {
        this.filterGroup = filterGroup;
    }

    public String getSmartQuery() {
        return smartQuery;
    }

    public void setSmartQuery(String smartQuery) {
        this.smartQuery = smartQuery;
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

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }

    public Boolean getIncludeSensitive() {
        return includeSensitive;
    }

    public void setIncludeSensitive(Boolean includeSensitive) {
        this.includeSensitive = includeSensitive;
    }
}
