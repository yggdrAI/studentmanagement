package com.sms.dto.student.filter;

import java.util.ArrayList;
import java.util.List;

public class StudentFilterNode {

    private String logic;
    private String field;
    private String operator;
    private Object value;
    private List<StudentFilterNode> filters = new ArrayList<>();

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<StudentFilterNode> getFilters() {
        return filters;
    }

    public void setFilters(List<StudentFilterNode> filters) {
        this.filters = filters == null ? new ArrayList<>() : filters;
    }

    public boolean isGroup() {
        return filters != null && !filters.isEmpty();
    }
}
