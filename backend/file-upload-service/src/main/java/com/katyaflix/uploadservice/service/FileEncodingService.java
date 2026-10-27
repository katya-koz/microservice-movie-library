package com.katyaflix.uploadservice.service;

import com.katyaflix.uploadservice.dto.mediaenrichment.MediaEnrichment;
import com.katyaflix.uploadservice.dto.mediaenrichment.SubtitleEnrichment;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;


/**
 * Output: single MP4 (H.264 + AAC, faststart). Writes to video.mp4.part and
 * only renames to video.mp4 after ffmpeg exits 0 and the duration checks out.
 * All commands are argument lists, never shell strings.
 */
@Service
public class FileEncodingService {

    private static final Logger log = LoggerFactory.getLogger(FileEncodingService.class);
    private final LanguageDetector languageDetector = LanguageDetectorBuilder.fromLanguages(
            Language.ENGLISH,
            Language.FRENCH,
            Language.SPANISH,
            Language.GERMAN,
            Language.ITALIAN,
            Language.RUSSIAN,
            Language.DANISH,
            Language.SWEDISH
    ).build();

    private final String ffmpeg;
    private final String ffprobe;
    private final Duration timeout;
    private final Set<Process> running = ConcurrentHashMap.newKeySet();

    public FileEncodingService(
            @Value("${encoding.ffmpeg-path:ffmpeg}") String ffmpeg,
            @Value("${encoding.ffprobe-path:ffprobe}") String ffprobe,
            @Value("${encoding.timeout-minutes:360}") long timeoutMinutes
    ) {
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
        this.timeout = Duration.ofMinutes(timeoutMinutes);
    }

    public MediaEnrichment encodeVideo(
            UUID id,
            Path source,
            Path outDir,
            IntConsumer onProgress
    ) throws IOException, InterruptedException {

        Files.createDirectories(outDir);

        Path part = outDir.resolve("video.mp4.part");
        Path out = outDir.resolve("video.mp4");

        Files.deleteIfExists(part);
        Files.deleteIfExists(out);

        double sourceSeconds = probeDurationSeconds(source);

        if (sourceSeconds <= 0) {
            throw new IOException("Source has no readable duration (corrupt or not a video?)");
        }

        List<String> cmd = List.of(
                ffmpeg, "-y", "-nostdin", "-v", "error",
                "-i", source.toString(),
                "-map", "0:v:0", "-map", "0:a:0?",
                "-c:v", "libx264", "-preset", "medium", "-crf", "21", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-b:a", "160k",
                "-movflags", "+faststart",
                "-f", "mp4",
                "-progress", "pipe:1", "-nostats",
                part.toString()
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectError(outDir.resolve("ffmpeg.log").toFile());

        Process p = pb.start();
        running.add(p);

        try {
            Instant deadline = Instant.now().plus(timeout);
            long lastReport = 0;

            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {

                String line;

                while ((line = r.readLine()) != null) {
                    if (Instant.now().isAfter(deadline)) {
                        p.destroyForcibly();
                        throw new IOException("ffmpeg timed out after " + timeout);
                    }

                    if (line.startsWith("out_time_us=")) {
                        try {
                            double done = Long.parseLong(line.substring(12)) / 1_000_000.0;
                            long now = System.currentTimeMillis();

                            if (now - lastReport > 5000) {
                                onProgress.accept(
                                        (int) Math.min(99, done / sourceSeconds * 100)
                                );
                                lastReport = now;
                            }
                        } catch (NumberFormatException ignored) {
                            // "N/A" early in the run
                        }
                    }
                }
            }

            int exit = p.waitFor();

            if (exit != 0) {
                throw new IOException(
                        "ffmpeg exited with " + exit + ": "
                                + tail(outDir.resolve("ffmpeg.log"))
                );
            }

        } catch (InterruptedException e) {
            p.destroyForcibly();
            throw e;
        } finally {
            running.remove(p);
        }

        // Verify output duration before declaring success
        double outSeconds = probeDurationSeconds(part);
        double tolerance = Math.max(2.0, sourceSeconds * 0.01);

        if (Math.abs(outSeconds - sourceSeconds) > tolerance) {
            throw new IOException(
                    "Output duration " + outSeconds
                            + "s does not match source " + sourceSeconds + "s"
            );
        }

        Files.move(part, out, StandardCopyOption.ATOMIC_MOVE);

        onProgress.accept(100);

        // Build and return the enrichment record
        return new MediaEnrichment(
                id,
                out.toString(),
                source.getFileName().toString(),
                "mp4",
                "h264",
                probeAudioCodec(out),
                probeResolution(out),
                (int) Math.round(outSeconds),
                Files.size(out),
                Instant.now()
        );
    }

    public SubtitleEnrichment convertSubtitle(
            UUID id,
            Path source,
            Path outDir
    ) throws IOException, InterruptedException {

        Files.createDirectories(outDir);

        Path out = outDir.resolve(id + ".vtt");
        Files.deleteIfExists(out);

        run(
                List.of(
                        ffmpeg, "-y", "-nostdin", "-v", "error",
                        "-i", source.toString(),
                        out.toString()
                ),
                Duration.ofMinutes(2)
        );

        if (!Files.exists(out) || Files.size(out) == 0) {
            throw new IOException("Subtitle conversion produced no output");
        }

        String text = Files.readString(out, StandardCharsets.UTF_8);

        // Remove WebVTT headers, timestamps, and cue markup.
        String dialogue = text
                .replaceAll("(?m)^WEBVTT.*$", "")
                .replaceAll("(?m)^NOTE.*$", "")
                .replaceAll("(?m)^\\d+$", "")
                .replaceAll("(?m)^\\d{2}:\\d{2}.*$", "")
                .replaceAll("<[^>]+>", "")
                .trim();

        String languageCode = detectLanguage(dialogue);
        String label = languageName(languageCode);

        return new SubtitleEnrichment(
                id,
                source.getFileName().toString(),
                out.toString(),
                languageCode,
                label,
                "vtt",
                Instant.now()
        );
    }

    private String detectLanguage(String text) {
        if (text == null || text.isBlank() || text.length() < 20) {
            return "und";
        }



        Language language = languageDetector.detectLanguageOf(text);

        return language.getIsoCode639_1() != null
                ? language.getIsoCode639_1().name().toLowerCase()
                : "und";
    }

    private String languageName(String code) {
        if (code == null || code.equals("und")) {
            return "Unknown";
        }

        return switch (code) {
            case "en" -> "English";
            case "fr" -> "French";
            case "de" -> "German";
            case "es" -> "Spanish";
            case "it" -> "Italian";
            case "ja" -> "Japanese";
            case "ko" -> "Korean";
            case "zh" -> "Chinese";
            case "ru" -> "Russian";
            default -> code;
        };
    }

    private String probeAudioCodec(Path file)
            throws IOException, InterruptedException {

        List<String> cmd = List.of(
                ffprobe,
                "-v", "error",
                "-select_streams", "a:0",
                "-show_entries", "stream=codec_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.toString()
        );

        Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        String result;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            result = reader.readLine();
        }

        int exit = p.waitFor();

        if (exit != 0) {
            throw new IOException("Failed to probe audio codec for " + file);
        }

        // Videos without an audio stream are valid.
        return result == null || result.isBlank() ? null : result.trim();
    }

    private String probeResolution(Path file)
            throws IOException, InterruptedException {

        List<String> cmd = List.of(
                ffprobe,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=s=x:p=0",
                file.toString()
        );

        Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        String result;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            result = reader.readLine();
        }

        int exit = p.waitFor();

        if (exit != 0 || result == null || result.isBlank()) {
            throw new IOException("Failed to probe video resolution for " + file);
        }

        return result.trim();
    }

    private double probeDurationSeconds(Path file) throws IOException, InterruptedException {
        String out = run(List.of(ffprobe, "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", file.toString()), Duration.ofMinutes(1)).trim();
        try {
            return Double.parseDouble(out);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String run(List<String> cmd, Duration limit) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(cmd));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        running.add(p);
        try {
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(limit.toSeconds(), TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("Command timed out: " + cmd.get(0));
            }
            if (p.exitValue() != 0) {
                throw new IOException(cmd.get(0) + " failed: " + output);
            }
            return output;
        } finally {
            running.remove(p);
        }
    }

    private String tail(Path logFile) {
        try {
            List<String> lines = Files.readAllLines(logFile);
            return String.join("\n", lines.subList(Math.max(0, lines.size() - 10), lines.size()));
        } catch (IOException e) {
            return "(no ffmpeg log)";
        }
    }

    @PreDestroy
    public void killAll() {
        running.forEach(Process::destroyForcibly);
        log.info("Killed {} running process(es)", running.size());
    }
}