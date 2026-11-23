package com.excelviewer.model;

public record MasterRecord(
        String nomenclature,
        String instrumentType,
        String signalType,
        String component,
        String make,
        String model
) {
}
