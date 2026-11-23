package com.excelviewer.model;

import java.util.List;

public record QualificationDocument(
        QualificationType type,
        List<String> headers,
        List<QualificationRow> rows,
        int matchedCount,
        int missingCount,
        boolean masterProvided,
        String errorMessage
) {

    public static QualificationDocument empty(QualificationType type) {
        return new QualificationDocument(type, List.of(), List.of(), 0, 0, false, null);
    }

    public static QualificationDocument failed(QualificationType type, String errorMessage) {
        return new QualificationDocument(type, List.of(), List.of(), 0, 0, true, errorMessage);
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isBlank();
    }
}
