package com.excelviewer.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AttextParser {

    private static final Pattern RECORD = Pattern.compile("'([^']*)','([^']*)','([^']*)'");

    public record AttextLine(String processFunction, String letter, String tag) {
    }

    public List<AttextLine> parse(File file) throws IOException {
        String text = readText(file);
        List<AttextLine> records = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            Matcher matcher = RECORD.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String processFunction = matcher.group(1).trim();
            String letter = TextNormalizer.cleanTag(matcher.group(2));
            String tag = TextNormalizer.cleanTag(matcher.group(3));
            if (letter.isEmpty() || tag.isEmpty()) {
                continue;
            }
            records.add(new AttextLine(processFunction, letter, tag));
        }
        return records;
    }

    private static String readText(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        Charset charset = Charset.isSupported("windows-1252")
                ? Charset.forName("windows-1252")
                : StandardCharsets.ISO_8859_1;
        return new String(bytes, charset);
    }
}
