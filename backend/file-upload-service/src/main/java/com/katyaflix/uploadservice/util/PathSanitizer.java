package com.katyaflix.uploadservice.util;

import java.util.regex.Pattern;

public final class PathSanitizer {

    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    private PathSanitizer() {}

    public static String sanitize(String input) {
        String cleaned = ILLEGAL_CHARS.matcher(input).replaceAll("").trim();
        return cleaned.isEmpty() ? "untitled" : cleaned;
    }

    /** Builds a "<Title> (<Year>)" folder name — the same convention
     * scan_library.py and the rest of the pipeline expect. */
    public static String titleYearFolder(String title, String year) {
        String safeTitle = sanitize(title);
        if (year == null || year.isBlank()) return safeTitle;
        return safeTitle + " (" + year + ")";
    }

    public static String yearFromIsoDate(String isoDate) {
        if (isoDate == null || isoDate.length() < 4) return null;
        return isoDate.substring(0, 4);
    }
}
