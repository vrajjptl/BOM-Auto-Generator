package com.excelviewer.service;

import com.excelviewer.model.BomItem;
import com.excelviewer.model.DqItem;
import com.excelviewer.model.GenerationResult;
import com.excelviewer.model.MasterRecord;
import com.excelviewer.model.QualificationDocument;
import com.excelviewer.model.QualificationRow;
import com.excelviewer.model.QualificationType;
import com.excelviewer.model.TagRecord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneratorService {

    public GenerationResult generate(File masterFile, File attextFile) throws IOException {
        return generate(masterFile, attextFile, Map.of(), null, null);
    }

    public GenerationResult generate(File masterFile, File attextFile,
                                     Map<QualificationType, File> qualificationFiles) throws IOException {
        return generate(masterFile, attextFile, qualificationFiles, null, null);
    }

    public GenerationResult generate(File masterFile, File attextFile,
                                     Map<QualificationType, File> qualificationFiles,
                                     File dqMasterFile) throws IOException {
        return generate(masterFile, attextFile, qualificationFiles, null, dqMasterFile);
    }

    public GenerationResult generate(File masterFile, File attextFile,
                                     Map<QualificationType, File> qualificationFiles,
                                     File bomMasterFile,
                                     File dqMasterFile) throws IOException {
        Map<String, MasterRecord> master = new MasterBomReader().read(masterFile);
        List<AttextParser.AttextLine> attextLines = new AttextParser().parse(attextFile);

        List<TagRecord> tags = new ArrayList<>();
        int matched = 0;
        int missing = 0;

        for (AttextParser.AttextLine line : attextLines) {
            String generated = (line.letter() + " " + line.tag()).replaceAll("\\s+", " ").trim();
            MasterRecord detail = master.get(TextNormalizer.normalizeKey(generated));
            String[] parts = line.tag().split("_", -1);
            String status = detail != null ? "FOUND" : "NEW";
            if (detail != null) {
                matched++;
            } else {
                missing++;
            }
            tags.add(new TagRecord(
                    line.processFunction(),
                    line.tag(),
                    line.letter(),
                    lineKey(line.letter()),
                    partAt(parts, 1),
                    partAt(parts, 2),
                    generated,
                    detail == null ? "" : detail.nomenclature(),
                    detail == null ? "" : detail.instrumentType(),
                    detail == null ? "" : detail.signalType(),
                    detail == null ? "" : detail.component(),
                    detail == null ? "" : detail.make(),
                    detail == null ? "" : detail.model(),
                    status
            ));
        }

        if (tags.isEmpty()) {
            throw new IOException("No valid ATTEXT records were detected. Expected three single-quoted values per record.");
        }

        BomBuild bomBuild = buildBomSafe(tags, bomMasterFile);
        DqBuild dqBuild = buildDqSafe(tags, dqMasterFile);
        return new GenerationResult(
                tags,
                bomBuild.items,
                dqBuild.items,
                buildQualifications(tags, qualificationFiles),
                matched,
                missing,
                joinNotes(bomBuild.note, dqBuild.note)
        );
    }

    private static BomBuild buildBomSafe(List<TagRecord> tags, File bomMasterFile) {
        if (bomMasterFile == null) {
            return new BomBuild(buildBom(tags), "");
        }
        try {
            return new BomBuild(buildBomFromMaster(tags, bomMasterFile), "");
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "Unable to read Master BOM." : ex.getMessage();
            return new BomBuild(buildBom(tags),
                    "Master BOM could not be read (" + message + "). BOM was built from the Master Tag List instead.");
        }
    }

    private static List<BomItem> buildBomFromMaster(List<TagRecord> tags, File bomMasterFile) throws IOException {
        Map<String, QualificationRow> master = new MasterQualificationReader().read(bomMasterFile);
        List<TagRecord> sourced = new ArrayList<>();
        for (TagRecord record : tags) {
            if (!record.found()) {
                continue;
            }
            QualificationRow masterRow = master.get(TextNormalizer.normalizeKey(record.generatedTag()));
            if (masterRow == null) {
                continue;
            }
            String[] fields = bomFields(masterRow);
            sourced.add(new TagRecord(
                    record.processFunction(),
                    record.tagNumber(),
                    record.letter(),
                    record.lineKey(),
                    record.machineSection(),
                    record.runningNumber(),
                    record.generatedTag(),
                    record.nomenclature(),
                    record.instrumentType(),
                    record.signalType(),
                    fields[0],
                    fields[1],
                    fields[2],
                    record.status()
            ));
        }
        return buildBom(sourced);
    }

    private static String[] bomFields(QualificationRow row) {
        String component = namedField(row, List.of("component", "item", "part", "material"));
        String make = namedField(row, List.of("make", "manufacturer"));
        String model = namedField(row, List.of("model"));
        if (component != null || make != null || model != null) {
            return new String[] {
                    component != null ? component : positionalBom(row, 0),
                    make != null ? make : positionalBom(row, 1),
                    model != null ? model : positionalBom(row, 2)
            };
        }
        if (row.headers().size() >= 6) {
            return new String[] { row.valueAt(3), row.valueAt(4), row.valueAt(5) };
        }
        return new String[] { row.valueAt(0), row.valueAt(1), row.valueAt(2) };
    }

    private static String positionalBom(QualificationRow row, int compactIndex) {
        if (row.headers().size() >= 6) {
            return row.valueAt(3 + compactIndex);
        }
        return row.valueAt(compactIndex);
    }

    private static String namedField(QualificationRow row, List<String> aliases) {
        List<String> headers = row.headers();
        for (int i = 0; i < headers.size(); i++) {
            String header = TextNormalizer.normalizeKey(headers.get(i));
            for (String alias : aliases) {
                if (header.equals(TextNormalizer.normalizeKey(alias))) {
                    return row.valueAt(i);
                }
            }
        }
        return null;
    }

    private static String joinNotes(String... notes) {
        List<String> parts = new ArrayList<>();
        for (String note : notes) {
            if (note != null && !note.isBlank()) {
                parts.add(note);
            }
        }
        return String.join("  •  ", parts);
    }

    private static List<BomItem> buildBom(List<TagRecord> tags) {
        Map<String, BomAccumulator> groups = new LinkedHashMap<>();
        for (TagRecord record : tags) {
            if (!record.found()) {
                continue;
            }
            String component = TextNormalizer.cellText(record.component());
            String make = TextNormalizer.cellText(record.make());
            String model = TextNormalizer.cellText(record.model());
            if (component.isEmpty() && make.isEmpty() && model.isEmpty()) {
                continue;
            }
            String key = TextNormalizer.normalizeKey(component)
                    + "|" + TextNormalizer.normalizeKey(make)
                    + "|" + TextNormalizer.normalizeKey(model);
            BomAccumulator group = groups.get(key);
            if (group == null) {
                group = new BomAccumulator(
                        blankLabel(component),
                        blankLabel(make),
                        blankLabel(model)
                );
                groups.put(key, group);
            }
            group.quantity++;
        }

        List<BomAccumulator> sorted = new ArrayList<>(groups.values());
        sorted.sort(Comparator
                .comparing((BomAccumulator item) -> item.component, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(item -> item.make, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(item -> item.model, String.CASE_INSENSITIVE_ORDER));

        List<BomItem> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            BomAccumulator item = sorted.get(i);
            result.add(new BomItem(i + 1, item.component, item.make, item.model, item.quantity));
        }
        return result;
    }

    private static DqBuild buildDqSafe(List<TagRecord> tags, File dqMasterFile) {
        if (dqMasterFile == null) {
            return new DqBuild(buildDq(tags), "");
        }
        try {
            return new DqBuild(buildDqFromMaster(tags, dqMasterFile), "");
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "Unable to read Master DQ." : ex.getMessage();
            return new DqBuild(buildDq(tags),
                    "Master DQ could not be read (" + message + "). DQ was built from the Master Tag List instead.");
        }
    }

    private static List<DqItem> buildDq(List<TagRecord> tags) {
        Map<String, DqItem> unique = new LinkedHashMap<>();
        for (TagRecord record : tags) {
            if (!record.found()) {
                continue;
            }
            String key = TextNormalizer.normalizeKey(record.generatedTag());
            unique.putIfAbsent(key, new DqItem(
                    record.generatedTag(),
                    record.nomenclature(),
                    record.instrumentType(),
                    record.signalType(),
                    record.component(),
                    record.make(),
                    record.model()
            ));
        }
        List<DqItem> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparing(DqItem::tag, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static List<DqItem> buildDqFromMaster(List<TagRecord> tags, File dqMasterFile) throws IOException {
        Map<String, QualificationRow> master = new MasterQualificationReader().read(dqMasterFile);
        Map<String, DqItem> unique = new LinkedHashMap<>();
        for (TagRecord record : tags) {
            if (!record.found()) {
                continue;
            }
            String key = TextNormalizer.normalizeKey(record.generatedTag());
            if (unique.containsKey(key)) {
                continue;
            }
            QualificationRow masterRow = master.get(key);
            if (masterRow == null) {
                unique.put(key, new DqItem(record.generatedTag(), "", "", "", "", "", ""));
            } else {
                unique.put(key, toDqItem(record.generatedTag(), masterRow));
            }
        }
        List<DqItem> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparing(DqItem::tag, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static DqItem toDqItem(String tag, QualificationRow row) {
        return new DqItem(
                tag,
                fieldFrom(row, List.of("nomenclature", "name", "description"), 0),
                fieldFrom(row, List.of("instrument type", "instrumenttype", "type", "inst type"), 1),
                fieldFrom(row, List.of("signal type", "signaltype", "signal"), 2),
                fieldFrom(row, List.of("panel/field material", "panel field material", "material", "component"), 3),
                fieldFrom(row, List.of("make"), 4),
                fieldFrom(row, List.of("model"), 5)
        );
    }

    private static String fieldFrom(QualificationRow row, List<String> aliases, int fallbackIndex) {
        List<String> headers = row.headers();
        for (int i = 0; i < headers.size(); i++) {
            String header = TextNormalizer.normalizeKey(headers.get(i));
            for (String alias : aliases) {
                if (header.equals(TextNormalizer.normalizeKey(alias))) {
                    return row.valueAt(i);
                }
            }
        }
        return row.valueAt(fallbackIndex);
    }

    private static String blankLabel(String value) {
        return value == null || value.isEmpty() ? "(Blank)" : value;
    }

    private static Map<QualificationType, QualificationDocument> buildQualifications(
            List<TagRecord> tags, Map<QualificationType, File> qualificationFiles) {
        Map<QualificationType, QualificationDocument> documents = new EnumMap<>(QualificationType.class);
        for (QualificationType type : QualificationType.values()) {
            File file = qualificationFiles == null ? null : qualificationFiles.get(type);
            if (file == null) {
                documents.put(type, QualificationDocument.empty(type));
                continue;
            }
            try {
                documents.put(type, buildQualification(type, tags, file));
            } catch (Exception ex) {
                documents.put(type, QualificationDocument.failed(type, ex.getMessage()));
            }
        }
        return documents;
    }

    private static QualificationDocument buildQualification(QualificationType type, List<TagRecord> tags, File file)
            throws IOException {
        Map<String, QualificationRow> master = new MasterQualificationReader().read(file);
        List<String> headers = master.values().stream().findFirst().map(QualificationRow::headers).orElse(List.of());
        Map<String, QualificationRow> unique = new LinkedHashMap<>();
        int matched = 0;
        int missing = 0;

        for (TagRecord record : tags) {
            if (!record.found()) {
                continue;
            }
            String key = TextNormalizer.normalizeKey(record.generatedTag());
            if (unique.containsKey(key)) {
                continue;
            }
            QualificationRow masterRow = master.get(key);
            if (masterRow == null) {
                missing++;
                unique.put(key, new QualificationRow(record.generatedTag(), "MISSING", headers, blankValues(headers.size())));
            } else {
                matched++;
                unique.put(key, new QualificationRow(record.generatedTag(), "MATCHED", masterRow.headers(), masterRow.values()));
            }
        }

        List<QualificationRow> rows = new ArrayList<>(unique.values());
        rows.sort(Comparator.comparing(QualificationRow::tag, String.CASE_INSENSITIVE_ORDER));
        return new QualificationDocument(type, headers, rows, matched, missing, true, null);
    }

    private static List<String> blankValues(int count) {
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add("");
        }
        return values;
    }

    private static String lineKey(String letter) {
        if (letter == null || letter.isEmpty()) {
            return "";
        }
        return letter.substring(0, Math.min(2, letter.length()));
    }

    private static String partAt(String[] parts, int index) {
        return index < parts.length ? parts[index] : "";
    }

    private static final class BomBuild {
        private final List<BomItem> items;
        private final String note;

        private BomBuild(List<BomItem> items, String note) {
            this.items = items;
            this.note = note == null ? "" : note;
        }
    }

    private static final class BomAccumulator {
        private final String component;
        private final String make;
        private final String model;
        private int quantity;

        private BomAccumulator(String component, String make, String model) {
            this.component = component;
            this.make = make;
            this.model = model;
        }
    }

    private static final class DqBuild {
        private final List<DqItem> items;
        private final String note;

        private DqBuild(List<DqItem> items, String note) {
            this.items = items;
            this.note = note == null ? "" : note;
        }
    }
}
