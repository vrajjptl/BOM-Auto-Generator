package com.excelviewer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record QualificationRow(String tag, String status, List<String> headers, List<String> values) {

    public String valueAt(int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        String value = values.get(index);
        return value == null ? "" : value;
    }

    public boolean matched() {
        return "MATCHED".equals(status);
    }

    public String searchText() {
        StringBuilder text = new StringBuilder(tag == null ? "" : tag);
        text.append(' ').append(status);
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                text.append(' ').append(value);
            }
        }
        return text.toString().toLowerCase(Locale.ROOT);
    }

    public List<String> fieldLabels() {
        return headers == null ? List.of() : headers;
    }

    public List<String> displayValues() {
        List<String> display = new ArrayList<>();
        display.add(tag);
        display.addAll(values);
        display.add(status);
        return display;
    }
}
