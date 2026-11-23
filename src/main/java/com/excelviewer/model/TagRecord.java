package com.excelviewer.model;

public record TagRecord(
        String processFunction,
        String tagNumber,
        String letter,
        String lineKey,
        String machineSection,
        String runningNumber,
        String generatedTag,
        String nomenclature,
        String instrumentType,
        String signalType,
        String component,
        String make,
        String model,
        String status
) {

    public boolean found() {
        return "FOUND".equals(status);
    }
}
