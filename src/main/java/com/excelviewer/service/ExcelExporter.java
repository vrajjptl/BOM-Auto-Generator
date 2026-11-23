package com.excelviewer.service;

import com.excelviewer.model.BomItem;
import com.excelviewer.model.TagRecord;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public final class ExcelExporter {

    public void exportTagList(List<TagRecord> tags, File destination) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PID Tag List");
            XSSFCellStyle headerStyle = headerStyle(workbook);
            XSSFCellStyle bodyStyle = bodyStyle(workbook);

            String[] headers = {"Generated Tag", "Nomenclature", "Status"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (TagRecord tag : tags) {
                Row row = sheet.createRow(rowIndex++);
                write(row, 0, tag.generatedTag(), bodyStyle);
                write(row, 1, tag.nomenclature(), bodyStyle);
                write(row, 2, tag.status(), bodyStyle);
            }

            sheet.setColumnWidth(0, 22 * 256);
            sheet.setColumnWidth(1, 48 * 256);
            sheet.setColumnWidth(2, 14 * 256);
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(tags.size(), 1), 0, 2));
            writeWorkbook(workbook, destination);
        }
    }

    public void exportBom(List<BomItem> items, File destination) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("BOM Summary");
            XSSFCellStyle headerStyle = headerStyle(workbook);
            XSSFCellStyle bodyStyle = bodyStyle(workbook);
            XSSFCellStyle qtyStyle = bodyStyle(workbook);
            qtyStyle.setAlignment(HorizontalAlignment.RIGHT);

            String[] headers = {"Sr. No.", "Component", "Make", "Model", "Quantity"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (BomItem item : items) {
                Row row = sheet.createRow(rowIndex++);
                write(row, 0, String.valueOf(item.serialNo()), bodyStyle);
                write(row, 1, item.component(), bodyStyle);
                write(row, 2, item.make(), bodyStyle);
                write(row, 3, item.model(), bodyStyle);
                row.createCell(4).setCellValue(item.quantity());
                row.getCell(4).setCellStyle(qtyStyle);
            }

            sheet.setColumnWidth(0, 10 * 256);
            sheet.setColumnWidth(1, 28 * 256);
            sheet.setColumnWidth(2, 24 * 256);
            sheet.setColumnWidth(3, 24 * 256);
            sheet.setColumnWidth(4, 12 * 256);
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(items.size(), 1), 0, 4));
            writeWorkbook(workbook, destination);
        }
    }

    private static void write(Row row, int column, String value, XSSFCellStyle style) {
        row.createCell(column).setCellValue(value == null ? "" : value);
        row.getCell(column).setCellStyle(style);
    }

    private static XSSFCellStyle headerStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static XSSFCellStyle bodyStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private static void writeWorkbook(XSSFWorkbook workbook, File destination) throws IOException {
        try (FileOutputStream output = new FileOutputStream(destination)) {
            workbook.write(output);
        }
    }
}
