package com.excelviewer.model;

public enum QualificationType {
    IQ("IQ", "Installation Qualification", "IQ-AUTOCOATER-001", "Installation_Qualification_Auto_Generated.doc"),
    OQ("OQ", "Operational Qualification", "OQ-AUTOCOATER-001", "Operational_Qualification_Auto_Generated.doc"),
    FS("FS", "Functional Specification", "FS-AUTOCOATER-001", "Functional_Specification_Auto_Generated.doc"),
    FAT("FAT", "Factory Acceptance Test", "FAT-AUTOCOATER-001", "Factory_Acceptance_Test_Auto_Generated.doc");

    private final String code;
    private final String title;
    private final String documentNumber;
    private final String exportFileName;

    QualificationType(String code, String title, String documentNumber, String exportFileName) {
        this.code = code;
        this.title = title;
        this.documentNumber = documentNumber;
        this.exportFileName = exportFileName;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String documentNumber() {
        return documentNumber;
    }

    public String exportFileName() {
        return exportFileName;
    }
}
