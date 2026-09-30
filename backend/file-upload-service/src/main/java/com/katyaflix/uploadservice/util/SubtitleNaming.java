package com.katyaflix.uploadservice.util;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Best-effort parsing of language / forced / SDH hints out of a subtitle's
 * original filename, following common release naming conventions, e.g.:
 *
 *   Movie.en.srt
 *   Movie.eng.forced.srt
 *   Movie.es.sdh.srt
 *
 * This is a heuristic, not a guarantee. Callers should treat "und"
 * (undetermined) as "we couldn't figure it out from the filename" rather
 * than an error.
 */
public final class SubtitleNaming {

    private SubtitleNaming() {}

    private static final Map<String, String> LANGUAGE_ALIASES = Map.ofEntries(
            Map.entry("eng", "en"), Map.entry("en", "en"),
            Map.entry("spa", "es"), Map.entry("es", "es"),
            Map.entry("fre", "fr"), Map.entry("fra", "fr"), Map.entry("fr", "fr"),
            Map.entry("ger", "de"), Map.entry("deu", "de"), Map.entry("de", "de"),
            Map.entry("ita", "it"), Map.entry("it", "it"),
            Map.entry("por", "pt"), Map.entry("pt", "pt"),
            Map.entry("rus", "ru"), Map.entry("ru", "ru"),
            Map.entry("jpn", "ja"), Map.entry("ja", "ja"),
            Map.entry("chi", "zh"), Map.entry("zho", "zh"), Map.entry("zh", "zh"),
            Map.entry("kor", "ko"), Map.entry("ko", "ko"),
            Map.entry("ara", "ar"), Map.entry("ar", "ar"),
            Map.entry("dut", "nl"), Map.entry("nld", "nl"), Map.entry("nl", "nl"),
            Map.entry("swe", "sv"), Map.entry("sv", "sv"),
            Map.entry("nor", "no"), Map.entry("no", "no"),
            Map.entry("dan", "da"), Map.entry("da", "da"),
            Map.entry("fin", "fi"), Map.entry("fi", "fi"),
            Map.entry("pol", "pl"), Map.entry("pl", "pl"),
            Map.entry("tur", "tr"), Map.entry("tr", "tr"),
            Map.entry("heb", "he"), Map.entry("he", "he"),
            Map.entry("hin", "hi"),
            Map.entry("tha", "th"), Map.entry("th", "th"),
            Map.entry("vie", "vi"), Map.entry("vi", "vi"),
            Map.entry("cze", "cs"), Map.entry("ces", "cs"), Map.entry("cs", "cs"),
            Map.entry("gre", "el"), Map.entry("ell", "el"), Map.entry("el", "el")
    );

    private static final Set<String> FORCED_TOKENS = Set.of("forced");
    private static final Set<String> SDH_TOKENS = Set.of("sdh", "cc");

    public record SubtitleNameInfo(String languageCode, String label, boolean forced, boolean sdh) {}

    public static SubtitleNameInfo parse(String filename) {
        String base = stripExtension(filename).toLowerCase(Locale.ROOT);
        String[] tokens = base.split("[._\\-\\s]+");

        String languageCode = "und";
        boolean forced = false;
        boolean sdh = false;

        for (String token : tokens) {
            if (LANGUAGE_ALIASES.containsKey(token)) {
                languageCode = LANGUAGE_ALIASES.get(token);
            } else if (FORCED_TOKENS.contains(token)) {
                forced = true;
            } else if (SDH_TOKENS.contains(token)) {
                sdh = true;
            }
        }

        return new SubtitleNameInfo(languageCode, buildLabel(languageCode, forced, sdh), forced, sdh);
    }

    private static String buildLabel(String languageCode, boolean forced, boolean sdh) {
        StringBuilder label = new StringBuilder(languageCode.toUpperCase(Locale.ROOT));
        if (sdh) label.append(" SDH");
        if (forced) label.append(" (Forced)");
        return label.toString();
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}