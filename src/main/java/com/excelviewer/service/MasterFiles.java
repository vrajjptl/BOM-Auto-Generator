package com.excelviewer.service;

import java.io.File;
import java.util.Locale;

public final class MasterFiles {

    private MasterFiles() {
    }

    public static boolean isDocx(File file) {
        return file != null && file.getName().toLowerCase(Locale.ROOT).endsWith(".docx");
    }

    public static boolean isLegacyDoc(File file) {
        return file != null
                && file.getName().toLowerCase(Locale.ROOT).endsWith(".doc")
                && !isDocx(file);
    }

    public static String wordExportName(String defaultName) {
        if (defaultName != null && defaultName.toLowerCase(Locale.ROOT).endsWith(".doc")) {
            return defaultName + "x";
        }
        return defaultName;
    }
}
