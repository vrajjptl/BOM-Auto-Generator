package com.excelviewer.service;

import com.excelviewer.model.QualificationRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads IQ / OQ / FS / FAT / DQ masters.
 * Excel: column 0 is the tag. Word (.docx): tag tables from the V3 master format.
 */
public final class MasterQualificationReader {

    public Map<String, QualificationRow> read(File file) throws IOException {
        if (MasterFiles.isLegacyDoc(file)) {
            throw new IOException("Please save " + file.getName() + " as .docx. Older .doc files are not supported.");
        }
        if (MasterFiles.isDocx(file)) {
            return new MasterWordReader().read(file);
        }
        try (Workbook workbook = WorkbookFactory.create(file, null, true)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Map<String, QualificationRow> master = new LinkedHashMap<>();

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                    continue;
                }
                int firstRowNum = sheet.getFirstRowNum();
                int lastRowNum = sheet.getLastRowNum();
                Row firstRow = sheet.getRow(firstRowNum);
                if (firstRow == null) {
                    continue;
                }
                int columnCount = Math.max(firstRow.getLastCellNum(), 1);
                boolean headerRow = looksLikeHeader(cellValue(firstRow.getCell(0), formatter, evaluator));
                List<String> headers = headersFrom(firstRow, columnCount, headerRow, formatter, evaluator);
                int startRow = headerRow ? firstRowNum + 1 : firstRowNum;

                for (int rowIndex = startRow; rowIndex <= lastRowNum; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    String tag = TextNormalizer.cleanTag(cellValue(row.getCell(0), formatter, evaluator));
                    String key = TextNormalizer.normalizeKey(tag);
                    if (key.isEmpty()) {
                        continue;
                    }
                    List<String> values = new ArrayList<>();
                    for (int col = 1; col < columnCount; col++) {
                        values.add(TextNormalizer.cellText(cellValue(row.getCell(col), formatter, evaluator)));
                    }
                    master.put(key, new QualificationRow(tag, "MATCHED", headers, values));
                }
            }

            if (master.isEmpty()) {
                throw new IOException("No valid tag records were found in " + file.getName() + ".");
            }
            return master;
        }
    }

    private static boolean looksLikeHeader(String firstCell) {
        return TextNormalizer.looksLikeTagLabel(firstCell);
    }

    private static List<String> headersFrom(Row firstRow, int columnCount, boolean headerRow,
                                            DataFormatter formatter, FormulaEvaluator evaluator) {
        List<String> headers = new ArrayList<>();
        for (int col = 1; col < columnCount; col++) {
            if (headerRow) {
                String header = TextNormalizer.cellText(cellValue(firstRow.getCell(col), formatter, evaluator));
                headers.add(header.isBlank() ? "Column " + (col + 1) : header);
            } else {
                headers.add("Column " + (col + 1));
            }
        }
        return headers;
    }

    private static String cellValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator);
    }
}
