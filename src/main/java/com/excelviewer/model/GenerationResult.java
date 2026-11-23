package com.excelviewer.model;

import java.util.List;
import java.util.Map;

public record GenerationResult(
        List<TagRecord> tags,
        List<BomItem> bomItems,
        List<DqItem> dqItems,
        Map<QualificationType, QualificationDocument> qualifications,
        int matchedCount,
        int missingCount,
        String note
) {

    public int totalTags() {
        return tags.size();
    }

    public int bomQuantity() {
        return bomItems.stream().mapToInt(BomItem::quantity).sum();
    }

    public int dqReadyCount() {
        return (int) dqItems.stream().filter(DqItem::complete).count();
    }

    public int dqIncompleteCount() {
        return dqItems.size() - dqReadyCount();
    }

    public QualificationDocument qualification(QualificationType type) {
        return qualifications.getOrDefault(type, QualificationDocument.empty(type));
    }
}
