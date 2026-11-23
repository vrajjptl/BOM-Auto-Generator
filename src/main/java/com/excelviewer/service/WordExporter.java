package com.excelviewer.service;

import com.excelviewer.model.DqItem;
import com.excelviewer.model.QualificationDocument;
import com.excelviewer.model.QualificationRow;
import com.excelviewer.model.QualificationType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WordExporter {

    public void exportDq(List<DqItem> items, File destination) throws IOException {
        if (items.isEmpty()) {
            throw new IOException("Generate data before creating the DQ document.");
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.UK));
        List<TagGroup> groups = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            String name = items.get(i).tag().split(" ", 2)[0];
            if (name.isBlank()) {
                name = "Other";
            }
            TagGroup existing = null;
            for (TagGroup group : groups) {
                if (group.name.equals(name)) {
                    existing = group;
                    break;
                }
            }
            if (existing == null) {
                groups.add(new TagGroup(name, i + 1));
            }
        }

        StringBuilder body = new StringBuilder();
        body.append("<div class=\"cover\"><h1>AUTOCOATER DESIGN QUALIFICATION</h1>")
                .append("<table><tr><td><b>Doc. No.:</b></td><td>DQ-AUTOCOATER-001</td></tr>")
                .append("<tr><td><b>Version:</b></td><td>A</td></tr>")
                .append("<tr><td><b>Status:</b></td><td>Issued for Approval</td></tr>")
                .append("<tr><td><b>Date:</b></td><td>").append(today).append("</td></tr>")
                .append("<tr><td><b>Document Type:</b></td><td>Design Qualification</td></tr></table><br>")
                .append("<table><tr><td><b>Project:</b></td><td>AUTOCOATER</td></tr>")
                .append("<tr><td><b>Reference:</b></td><td>Master BOM &amp; Tag List / P&amp;ID ATTEXT</td></tr>")
                .append("<tr><td><b>Customer:</b></td><td>-</td></tr></table><br>")
                .append("<table><tr><th>Role</th><th>Name / Position / Signature</th><th>Date</th></tr>")
                .append("<tr><td>Approver</td><td>Name: ____________________<br>Position: Customer<br><br>Signature:</td><td>____________</td></tr>")
                .append("<tr><td>Reviewer / Author</td><td>Name: ____________________<br>Position: Vendor<br><br>Signature:</td><td>____________</td></tr></table></div>");

        body.append("<div class=\"revision\"><h1>REVISION HISTORY</h1>")
                .append("<table><tr><th>Revision</th><th>Date</th><th>Description of status and changes</th><th>Originated by</th></tr>")
                .append("<tr><td>A</td><td>").append(today).append("</td><td>Issued for Approval</td><td>To be assigned</td></tr>");
        for (int i = 0; i < 5; i++) {
            body.append("<tr><td>&nbsp;</td><td></td><td></td><td></td></tr>");
        }
        body.append("</table></div>");

        body.append("<div class=\"index\"><h1>TABLE OF CONTENTS</h1>")
                .append("<table><tr><th>Sr.</th><th>Tag Group</th><th>First DQ Item</th></tr>");
        for (int i = 0; i < groups.size(); i++) {
            TagGroup group = groups.get(i);
            body.append("<tr><td>").append(i + 1).append("</td><td>")
                    .append(TextNormalizer.htmlEscape(group.name)).append(" Tags</td><td>")
                    .append(group.start).append("</td></tr>");
        }
        body.append("</table></div>");

        for (DqItem item : items) {
            String heading = item.nomenclature() == null || item.nomenclature().isBlank()
                    ? item.tag()
                    : item.nomenclature();
            body.append("<div class=\"dq\"><h2>").append(TextNormalizer.htmlEscape(heading)).append("</h2>")
                    .append("<table><tr><th>Design Attribute</th><th>Specified Requirement</th><th>Passed</th><th>Failed</th></tr>")
                    .append(dqRow("Tag No.", item.tag()))
                    .append(dqRow("Nomenclature", item.nomenclature()))
                    .append(dqRow("Instrument Type", item.instrumentType()))
                    .append(dqRow("Signal Type", item.signalType()))
                    .append(dqRow("Panel/Field Material", item.material()))
                    .append(dqRow("Make", item.make()))
                    .append(dqRow("Model", item.model()))
                    .append("</table></div>");
        }

        String css = "<style>@page{size:A4;margin:14mm}body{font-family:Arial,sans-serif;font-size:10pt;color:#111}"
                + "h1{text-align:center;font-size:18pt;margin:12px}h2{text-align:center;font-size:12pt;page-break-after:avoid}"
                + "table{border-collapse:collapse;width:100%;margin:8px 0 18px}td,th{border:1px solid #222;padding:6px}"
                + "th{background:#d9eaf7}.cover{page-break-after:always}.revision{page-break-after:always}"
                + ".index{page-break-after:always}.dq{page-break-inside:avoid}.box{text-align:center;font-size:15pt;width:9%}"
                + ".label{font-weight:bold;width:24%}.value{width:58%}</style>";

        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" + css + "</head><body>"
                + body + "</body></html>";
        Files.writeString(destination.toPath(), html, StandardCharsets.UTF_8);
    }

    public void exportQualification(QualificationDocument document, File destination) throws IOException {
        if (document == null || !document.masterProvided()) {
            throw new IOException("Select a master file and generate data before creating this document.");
        }
        if (document.hasError()) {
            throw new IOException(document.errorMessage());
        }
        if (document.rows().isEmpty()) {
            throw new IOException("No matched " + document.type().code() + " records were generated.");
        }

        QualificationType type = document.type();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.UK));
        List<TagGroup> groups = new ArrayList<>();
        for (int i = 0; i < document.rows().size(); i++) {
            String name = document.rows().get(i).tag().split(" ", 2)[0];
            if (name.isBlank()) {
                name = "Other";
            }
            TagGroup existing = null;
            for (TagGroup group : groups) {
                if (group.name.equals(name)) {
                    existing = group;
                    break;
                }
            }
            if (existing == null) {
                groups.add(new TagGroup(name, i + 1));
            }
        }

        StringBuilder body = new StringBuilder();
        body.append("<div class=\"cover\"><h1>AUTOCOATER ").append(TextNormalizer.htmlEscape(type.title().toUpperCase(Locale.ROOT))).append("</h1>")
                .append("<table><tr><td><b>Doc. No.:</b></td><td>").append(TextNormalizer.htmlEscape(type.documentNumber())).append("</td></tr>")
                .append("<tr><td><b>Version:</b></td><td>A</td></tr>")
                .append("<tr><td><b>Status:</b></td><td>Issued for Approval</td></tr>")
                .append("<tr><td><b>Date:</b></td><td>").append(today).append("</td></tr>")
                .append("<tr><td><b>Document Type:</b></td><td>").append(TextNormalizer.htmlEscape(type.title())).append("</td></tr></table><br>")
                .append("<table><tr><td><b>Project:</b></td><td>AUTOCOATER</td></tr>")
                .append("<tr><td><b>Reference:</b></td><td>Master ").append(TextNormalizer.htmlEscape(type.code()))
                .append(" / Tag List / P&amp;ID ATTEXT</td></tr>")
                .append("<tr><td><b>Customer:</b></td><td>-</td></tr></table><br>")
                .append("<table><tr><th>Role</th><th>Name / Position / Signature</th><th>Date</th></tr>")
                .append("<tr><td>Approver</td><td>Name: ____________________<br>Position: Customer<br><br>Signature:</td><td>____________</td></tr>")
                .append("<tr><td>Reviewer / Author</td><td>Name: ____________________<br>Position: Vendor<br><br>Signature:</td><td>____________</td></tr></table></div>");

        body.append("<div class=\"revision\"><h1>REVISION HISTORY</h1>")
                .append("<table><tr><th>Revision</th><th>Date</th><th>Description of status and changes</th><th>Originated by</th></tr>")
                .append("<tr><td>A</td><td>").append(today).append("</td><td>Issued for Approval</td><td>To be assigned</td></tr>");
        for (int i = 0; i < 5; i++) {
            body.append("<tr><td>&nbsp;</td><td></td><td></td><td></td></tr>");
        }
        body.append("</table></div>");

        body.append("<div class=\"index\"><h1>TABLE OF CONTENTS</h1>")
                .append("<table><tr><th>Sr.</th><th>Tag Group</th><th>First Item</th></tr>");
        for (int i = 0; i < groups.size(); i++) {
            TagGroup group = groups.get(i);
            body.append("<tr><td>").append(i + 1).append("</td><td>")
                    .append(TextNormalizer.htmlEscape(group.name)).append(" Tags</td><td>")
                    .append(group.start).append("</td></tr>");
        }
        body.append("</table></div>");

        for (QualificationRow row : document.rows()) {
            String heading = row.tag();
            body.append("<div class=\"dq\"><h2>").append(TextNormalizer.htmlEscape(heading)).append("</h2>")
                    .append("<table><tr><th>Attribute</th><th>Specified Requirement</th><th>Passed</th><th>Failed</th></tr>")
                    .append(dqRow("Tag No.", row.tag()))
                    .append(dqRow("Status", row.status()));
            List<String> headers = row.headers();
            for (int i = 0; i < headers.size(); i++) {
                body.append(dqRow(headers.get(i), row.valueAt(i)));
            }
            body.append("</table></div>");
        }

        String css = "<style>@page{size:A4;margin:14mm}body{font-family:Arial,sans-serif;font-size:10pt;color:#111}"
                + "h1{text-align:center;font-size:18pt;margin:12px}h2{text-align:center;font-size:12pt;page-break-after:avoid}"
                + "table{border-collapse:collapse;width:100%;margin:8px 0 18px}td,th{border:1px solid #222;padding:6px}"
                + "th{background:#d9eaf7}.cover{page-break-after:always}.revision{page-break-after:always}"
                + ".index{page-break-after:always}.dq{page-break-inside:avoid}.box{text-align:center;font-size:15pt;width:9%}"
                + ".label{font-weight:bold;width:24%}.value{width:58%}</style>";

        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" + css + "</head><body>"
                + body + "</body></html>";
        Files.writeString(destination.toPath(), html, StandardCharsets.UTF_8);
    }

    private static String dqRow(String label, String value) {
        return "<tr><td class=\"label\">" + TextNormalizer.htmlEscape(label)
                + "</td><td class=\"value\">" + TextNormalizer.htmlEscape(value)
                + "</td><td class=\"box\">□</td><td class=\"box\">□</td></tr>";
    }

    private static final class TagGroup {
        private final String name;
        private final int start;

        private TagGroup(String name, int start) {
            this.name = name;
            this.start = start;
        }
    }
}
