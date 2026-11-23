package com.excelviewer.service;

import com.excelviewer.model.TagRecord;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Copies a Word master and keeps only tag tables / rows that match the generated Tag List.
 */
public final class WordMasterExporter {

    public void exportFiltered(File masterFile, Collection<TagRecord> tags, File destination) throws IOException {
        Set<String> allowed = foundTagKeys(tags);
        try (FileInputStream input = new FileInputStream(masterFile);
             XWPFDocument document = new XWPFDocument(input)) {
            filter(document, allowed);
            try (FileOutputStream output = new FileOutputStream(destination)) {
                document.write(output);
            }
        }
    }

    private static void filter(XWPFDocument document, Set<String> allowed) {
        List<IBodyElement> elements = document.getBodyElements();
        for (int i = elements.size() - 1; i >= 0; i--) {
            IBodyElement element = document.getBodyElements().get(i);
            if (!(element instanceof XWPFTable table)) {
                continue;
            }
            List<List<String>> grid = MasterWordReader.tableToGrid(table);
            if (grid.isEmpty() || grid.get(0).isEmpty()) {
                continue;
            }
            String first = grid.get(0).get(0);
            String second = grid.get(0).size() > 1 ? grid.get(0).get(1) : "";
            if (TextNormalizer.looksLikeTagLabel(first) && !TextNormalizer.extractLeadingTag(second).isEmpty()) {
                String key = TextNormalizer.normalizeKey(second);
                if (!allowed.contains(key)) {
                    String heading = grid.size() > 1 && !grid.get(1).isEmpty() ? grid.get(1).get(0) : "";
                    if (grid.size() > 1 && grid.get(1).size() > 1
                            && TextNormalizer.normalizeKey(grid.get(1).get(0)).equals("NOMENCLATURE")) {
                        heading = grid.get(1).get(1);
                    }
                    document.removeBodyElement(i);
                    if (heading != null && !heading.isBlank() && i > 0) {
                        IBodyElement previous = document.getBodyElements().get(i - 1);
                        if (previous instanceof XWPFParagraph paragraph
                                && heading.equalsIgnoreCase(paragraph.getText().trim())) {
                            document.removeBodyElement(i - 1);
                        }
                    }
                }
                continue;
            }
            int tagColumn = findTagColumn(grid.get(0));
            if (tagColumn >= 0) {
                removeUnmatchedColumnRows(table, tagColumn, allowed);
                continue;
            }
            if (embeddedTagCount(grid) >= 2) {
                removeUnmatchedEmbeddedRows(table, allowed);
            }
        }
    }

    private static void removeUnmatchedColumnRows(XWPFTable table, int tagColumn, Set<String> allowed) {
        List<XWPFTableRow> rows = table.getRows();
        for (int row = rows.size() - 1; row >= 1; row--) {
            String tag = tagColumn < rows.get(row).getTableCells().size()
                    ? TextNormalizer.cleanTag(rows.get(row).getCell(tagColumn).getText())
                    : "";
            if (tag.isEmpty()) {
                continue;
            }
            if (!allowed.contains(TextNormalizer.normalizeKey(tag))) {
                table.removeRow(row);
            }
        }
    }

    private static void removeUnmatchedEmbeddedRows(XWPFTable table, Set<String> allowed) {
        List<XWPFTableRow> rows = table.getRows();
        for (int row = rows.size() - 1; row >= 1; row--) {
            String tag = "";
            for (int col = 0; col < rows.get(row).getTableCells().size(); col++) {
                tag = TextNormalizer.extractLeadingTag(rows.get(row).getCell(col).getText());
                if (!tag.isEmpty()) {
                    break;
                }
            }
            if (!tag.isEmpty() && !allowed.contains(TextNormalizer.normalizeKey(tag))) {
                table.removeRow(row);
            }
        }
    }

    private static int findTagColumn(List<String> headerRow) {
        for (int col = 0; col < headerRow.size(); col++) {
            if (TextNormalizer.looksLikeTagLabel(headerRow.get(col))) {
                return col;
            }
        }
        return -1;
    }

    private static int embeddedTagCount(List<List<String>> grid) {
        int count = 0;
        for (int row = 1; row < grid.size(); row++) {
            for (String cell : grid.get(row)) {
                if (!TextNormalizer.extractLeadingTag(cell).isEmpty()) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static Set<String> foundTagKeys(Collection<TagRecord> tags) {
        Set<String> keys = new HashSet<>();
        if (tags == null) {
            return keys;
        }
        for (TagRecord tag : tags) {
            if (tag != null && tag.found()) {
                keys.add(TextNormalizer.normalizeKey(tag.generatedTag()));
            }
        }
        return keys;
    }
}
