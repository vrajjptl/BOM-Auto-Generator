package com.excelviewer.model;

public record DqItem(
        String tag,
        String nomenclature,
        String instrumentType,
        String signalType,
        String material,
        String make,
        String model
) {

    public boolean complete() {
        return isPresent(nomenclature) && isPresent(instrumentType) && isPresent(material);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
