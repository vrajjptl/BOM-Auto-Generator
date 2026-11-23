package com.excelviewer.service;

import com.excelviewer.model.MasterRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads every worksheet of the Master BOM Tag List.
 * Layout: Tag, Nomenclature, Instrument Type, Signal Type, Component, Make, Model.
 */
public final class MasterBomReader {

    public Map<String, MasterRecord> read(File file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file, null, true)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Map<String, MasterRecord> master = new LinkedHashMap<>();

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (sheet == null) {
                    continue;
                }
                for (Row row : sheet) {
                    String key = TextNormalizer.normalizeKey(cellValue(row.getCell(0), formatter, evaluator));
                    if (key.isEmpty()) {
                        continue;
                    }
                    master.put(key, new MasterRecord(
                            TextNormalizer.cellText(cellValue(row.getCell(1), formatter, evaluator)),
                            TextNormalizer.cellText(cellValue(row.getCell(2), formatter, evaluator)),
                            TextNormalizer.cellText(cellValue(row.getCell(3), formatter, evaluator)),
                            TextNormalizer.cellText(cellValue(row.getCell(4), formatter, evaluator)),
                            TextNormalizer.cellText(cellValue(row.getCell(5), formatter, evaluator)),
                            TextNormalizer.cellText(cellValue(row.getCell(6), formatter, evaluator))
                    ));
                }
            }

            if (master.isEmpty()) {
                throw new IOException("No valid tag records were found in the Master BOM file.");
            }
            return master;
        }
    }

    private static String cellValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator);
    }
}
