package com.excelviewer.service;

import com.excelviewer.model.QualificationRow;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads DQ / OQ / FS Word masters (.docx).
 * Supports vertical Tag No. tables, tag-column tables, and rows that start with a P&amp;ID tag.
 */
public final class MasterWordReader {

    public Map<String, QualificationRow> read(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(input)) {
            Map<String, QualificationRow> master = new LinkedHashMap<>();
            for (XWPFTable table : document.getTables()) {
                parseTable(tableToGrid(table), master);
            }
            if (master.isEmpty()) {
                throw new IOException("No tag records were found in " + file.getName() + ".");
            }
            return master;
        }
    }

    static List<List<String>> tableToGrid(XWPFTable table) {
        List<List<String>> grid = new ArrayList<>();
        if (table == null) {
            return grid;
        }
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                cells.add(TextNormalizer.cellText(cell.getText()));
            }
            grid.add(cells);
        }
        return grid;
    }

    static void parseTable(List<List<String>> grid, Map<String, QualificationRow> master) {
        if (grid.isEmpty() || grid.get(0).isEmpty()) {
            return;
        }
        String first = grid.get(0).get(0);
        String second = grid.get(0).size() > 1 ? grid.get(0).get(1) : "";
        if (TextNormalizer.looksLikeTagLabel(first) && !TextNormalizer.extractLeadingTag(second).isEmpty()) {
            parseAttributeTable(grid, master);
            return;
        }
        int tagColumn = findTagColumn(grid.get(0));
        if (tagColumn >= 0) {
            parseColumnTable(grid, tagColumn, master);
            return;
        }
        parseEmbeddedTagRows(grid, master);
    }

    private static void parseAttributeTable(List<List<String>> grid, Map<String, QualificationRow> master) {
        String tag = TextNormalizer.cleanTag(grid.get(0).get(1));
        List<String> headers = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int row = 1; row < grid.size(); row++) {
            List<String> cells = grid.get(row);
            String header = cells.isEmpty() ? "" : TextNormalizer.cellText(cells.get(0));
            if (header.isBlank()) {
                continue;
            }
            headers.add(header);
            values.add(cells.size() > 1 ? TextNormalizer.cellText(cells.get(1)) : "");
        }
        putOrMerge(master, tag, headers, values);
    }

    private static void parseColumnTable(List<List<String>> grid, int tagColumn, Map<String, QualificationRow> master) {
        List<String> headerRow = grid.get(0);
        List<String> headers = new ArrayList<>();
        List<Integer> valueColumns = new ArrayList<>();
        for (int col = 0; col < headerRow.size(); col++) {
            if (col == tagColumn) {
                continue;
            }
            String header = TextNormalizer.cellText(headerRow.get(col));
            headers.add(header.isBlank() ? "Column " + (col + 1) : header);
            valueColumns.add(col);
        }
        for (int row = 1; row < grid.size(); row++) {
            List<String> cells = grid.get(row);
            String tag = tagColumn < cells.size()
                    ? TextNormalizer.cleanTag(cells.get(tagColumn))
                    : "";
            if (TextNormalizer.normalizeKey(tag).isEmpty()) {
                continue;
            }
            List<String> values = new ArrayList<>();
            for (int col : valueColumns) {
                values.add(col < cells.size() ? TextNormalizer.cellText(cells.get(col)) : "");
            }
            putOrMerge(master, tag, headers, values);
        }
    }

    private static void parseEmbeddedTagRows(List<List<String>> grid, Map<String, QualificationRow> master) {
        int taggedRows = 0;
        for (int row = 1; row < grid.size(); row++) {
            if (!leadingTag(grid.get(row)).isEmpty()) {
                taggedRows++;
            }
        }
        if (taggedRows < 2) {
            return;
        }
        List<String> headerRow = grid.get(0);
        for (int row = 1; row < grid.size(); row++) {
            List<String> cells = grid.get(row);
            String tag = leadingTag(cells);
            if (tag.isEmpty()) {
                continue;
            }
            List<String> headers = new ArrayList<>();
            List<String> values = new ArrayList<>();
            for (int col = 0; col < Math.max(headerRow.size(), cells.size()); col++) {
                String header = col < headerRow.size() ? TextNormalizer.cellText(headerRow.get(col)) : "";
                if (header.isBlank()) {
                    header = col == 0 ? "Text" : "Column " + (col + 1);
                }
                headers.add(header);
                values.add(col < cells.size() ? TextNormalizer.cellText(cells.get(col)) : "");
            }
            putOrMerge(master, tag, headers, values);
        }
    }

    private static String leadingTag(List<String> cells) {
        for (String cell : cells) {
            String tag = TextNormalizer.extractLeadingTag(cell);
            if (!tag.isEmpty()) {
                return tag;
            }
        }
        return "";
    }

    private static int findTagColumn(List<String> headerRow) {
        for (int col = 0; col < headerRow.size(); col++) {
            if (TextNormalizer.looksLikeTagLabel(headerRow.get(col))) {
                return col;
            }
        }
        return -1;
    }

    private static void putOrMerge(Map<String, QualificationRow> master, String tag,
                                  List<String> headers, List<String> values) {
        String key = TextNormalizer.normalizeKey(tag);
        if (key.isEmpty()) {
            return;
        }
        QualificationRow existing = master.get(key);
        if (existing == null) {
            master.put(key, new QualificationRow(TextNormalizer.cleanTag(tag), "MATCHED", headers, values));
            return;
        }
        List<String> mergedHeaders = new ArrayList<>(existing.headers());
        List<String> mergedValues = new ArrayList<>(existing.values());
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            String value = i < values.size() ? values.get(i) : "";
            int index = indexOfHeader(mergedHeaders, header);
            if (index < 0) {
                mergedHeaders.add(header);
                mergedValues.add(value);
            } else if ((mergedValues.get(index) == null || mergedValues.get(index).isBlank())
                    && value != null && !value.isBlank()) {
                mergedValues.set(index, value);
            } else if (value != null && !value.isBlank() && !mergedValues.get(index).contains(value)) {
                mergedValues.set(index, mergedValues.get(index) + " | " + value);
            }
        }
        master.put(key, new QualificationRow(existing.tag(), "MATCHED", mergedHeaders, mergedValues));
    }

    private static int indexOfHeader(List<String> headers, String header) {
        String key = TextNormalizer.normalizeKey(header);
        for (int i = 0; i < headers.size(); i++) {
            if (TextNormalizer.normalizeKey(headers.get(i)).equals(key)) {
                return i;
            }
        }
        return -1;
    }
}
