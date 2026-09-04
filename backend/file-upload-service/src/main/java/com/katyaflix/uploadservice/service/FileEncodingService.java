package com.katyaflix.uploadservice.service;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.katyaflix.uploadservice.dto.CatalogEventDtos;
import com.katyaflix.uploadservice.dto.EncodedMediaFile;
import com.katyaflix.uploadservice.dto.MediaProbeResult;
import com.katyaflix.uploadservice.messaging.CatalogTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class FileEncodingService {

    private static final Logger log = LoggerFactory.getLogger(FileEncodingService.class);

    private static final String FFMPEG = "ffmpeg";
    private static final String FFPROBE = "ffprobe";

    private static final String SUPPORTED_VIDEO_CODEC_H264 = "h264";
    private static final String SUPPORTED_VIDEO_CODEC_HEVC = "hevc";
    private static final String SUPPORTED_AUDIO_CODEC_AAC = "aac";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FileEncodingService(KafkaTemplate<String, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }


    public void sendMediaEnrichmentEvent(UUID fileUploadId, UUID movieId, UUID episodeId, String originalFilename, EncodedMediaFile encodedFile) {

        if (fileUploadId == null) {
            throw new IllegalArgumentException("fileUploadId cannot be null");
        }
        if (encodedFile == null) {
            throw new IllegalArgumentException("Encoded media file cannot be null");
        }
        Instant encodedAt = Instant.now();
        String resolution = null;
        if (encodedFile.width() != null&& encodedFile.height() != null) {
            resolution = encodedFile.width() + "x" + encodedFile.height();
        }

        Integer durationSeconds = null;
        if (encodedFile.durationSeconds() != null) {
            durationSeconds =  (int) Math.round(encodedFile.durationSeconds());
        }

        CatalogEventDtos.MediaFileEnrichmentDto enrichmentDto =
                new CatalogEventDtos.MediaFileEnrichmentDto(
                        movieId,
                        episodeId,
                        originalFilename,
                        encodedFile.container(),
                        encodedFile.videoCodec(),
                        encodedFile.audioCodec(),
                        resolution,
                        durationSeconds,
                        encodedFile.sizeBytes(),
                        encodedAt
                );

        CatalogEventDtos.MediaFileEnrichmentEvent event =
                new CatalogEventDtos.MediaFileEnrichmentEvent(
                        fileUploadId,
                        encodedAt,
                        enrichmentDto
                );

        kafkaTemplate.send(CatalogTopics.MEDIA_ENRICHMENT_REQUESTED, fileUploadId.toString(), event).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Failed to send media enrichment event. fileUploadId={},originalFilename={}", fileUploadId, originalFilename, exception);
                return;
            }

            log.info(
                    "Sent media enrichment event. fileUploadId={}, originalFilename={}, topic={}, partition={}, offset={}",
                    fileUploadId,
                    originalFilename,
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }



    public List<EncodedMediaFile> encodeAll(Map<String, Path> files) throws IOException {

        List<EncodedMediaFile> encodedFiles =  new ArrayList<>();

        for (Map.Entry<String, Path> entry : files.entrySet()) {
            encodedFiles.add( encode(entry.getValue()) );
        }

        return encodedFiles;
    }


    public List<EncodedMediaFile> encodeAll( List<Path> files
    ) throws IOException {

        List<EncodedMediaFile> encodedFiles = new java.util.ArrayList<>();

        for (Path file : files) {
            encodedFiles.add(encode(file));
        }

        return encodedFiles;
    }


    public EncodedMediaFile encode(Path input) throws IOException {

        if (input == null) {
            throw new IllegalArgumentException(  "Input path cannot be null" );
        }

        if (!Files.exists(input)) {
            throw new IOException( "Media file does not exist: " + input );
        }

        if (!Files.isRegularFile(input)) {
            throw new IOException( "Media path is not a regular file: " + input );
        }

        log.info("Inspecting media file: {}", input);

        MediaProbeResult probe = probe(input);

        log.info(
                "Media: {} | video={} | audio={} | container={}",
                input,
                probe.videoCodec(),
                probe.audioCodec(),
                probe.container()
        );

        EncodingPlan plan = determinePlan(probe);
        log.info("Encoding plan for {}: {}", input, plan);

        String tmdbId = extractTmdbId(input);

        switch (plan) {
            case COPY -> {
                return EncodedMediaFile.from( probe, tmdbId );
            }
            case REMUX -> {
                Path output = temporaryOutput(input);
                runProcess(List.of(
                        FFMPEG,
                        "-y",
                        "-loglevel", "warning",
                        "-i", input.toString(),
                        "-map", "0:v",
                        "-map", "0:a",
                        "-c", "copy",
                        output.toString()
                ));

                return replaceOriginal(input, output, tmdbId);
            }

            case TRANSCODE_VIDEO -> {
                Path output = temporaryOutput(input);
                runProcess(List.of(
                        FFMPEG,
                        "-y",
                        "-loglevel", "warning",
                        "-i", input.toString(),
                        "-map", "0:v",
                        "-map", "0:a",
                        "-c:v", "libx264",
                        "-c:a", "copy",
                        output.toString()
                ));

                return replaceOriginal(input, output, tmdbId);
            }

            case TRANSCODE_AUDIO -> {
                Path output = temporaryOutput(input);

                runProcess(List.of(
                        FFMPEG,
                        "-y",
                        "-loglevel", "warning",
                        "-i", input.toString(),
                        "-map", "0:v",
                        "-map", "0:a",
                        "-c:v", "copy",
                        "-c:a", "aac",
                        output.toString()
                ));

                return replaceOriginal(input, output, tmdbId);
            }

            case TRANSCODE_BOTH -> {
                Path output = temporaryOutput(input);

                runProcess(List.of(
                        FFMPEG,
                        "-y",
                        "-loglevel", "warning",
                        "-i", input.toString(),
                        "-map", "0:v",
                        "-map", "0:a",
                        "-c:v", "libx264",
                        "-c:a", "aac",
                        output.toString()
                ));

                return replaceOriginal(input, output, tmdbId);
            }

            default -> throw new IllegalStateException( "Unhandled encoding plan: " + plan);
        }
    }


    private String extractTmdbId(Path input) throws IOException {
        // extract the tmdb id from path
        Path parent = input.getParent();

        if (parent == null) {
            throw new IOException("Cannot determine TMDB ID from path: " + input);
        }

        Path tmdbDirectory = parent.getFileName();

        if (tmdbDirectory == null) {
            throw new IOException("Cannot determine TMDB ID from path: " + input);
        }

        String tmdbId = tmdbDirectory.toString();

        if (tmdbId.isBlank()) {
            throw new IOException("TMDB ID directory is empty: " + input);
        }
        return tmdbId;
    }


    private EncodedMediaFile replaceOriginal(
            Path original,
            Path temporary,
            String tmdbId
    ) throws IOException {

        try {

            if (!Files.exists(temporary)) {
                throw new IOException("FFmpeg completed but output file does not exist: "+ temporary);
            }

            Files.delete(original);

            Path finalPath = original.resolveSibling(removeExtension(original.getFileName().toString()) + ".mp4");

            Files.move(temporary,finalPath);

            log.info("Encoding successful: {} -> {}",original,finalPath);

            /*
             * probe the actual resulting file so that the returned
             * EncodedMediaFile contains the metadata of the final file
             */
            MediaProbeResult finalProbe = probe(finalPath);

            return EncodedMediaFile.from(finalProbe,tmdbId);

        } catch (Exception e) {
            Files.deleteIfExists(temporary);
            throw e;
        }
    }



    private MediaProbeResult probe(Path input) throws IOException {

        String videoOutput = runProcess(List.of(
                FFPROBE,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries",
                "stream=codec_name,width,height,bit_rate,r_frame_rate",
                "-of", "default=noprint_wrappers=1",
                input.toString()
        )).stdout();

        String formatOutput = runProcess(List.of(
                FFPROBE,
                "-v", "error",
                "-show_entries",
                "format=format_name,duration,bit_rate",
                "-of", "default=noprint_wrappers=1",
                input.toString()
        )).stdout();

        String audioCodec = probeAudioCodec(input);

        String videoCodec = value(videoOutput, "codec_name");
        Integer width = integerValue(videoOutput, "width");
        Integer height = integerValue(videoOutput, "height");
        Long bitrate = longValue(formatOutput, "bit_rate");
        String frameRate = value(videoOutput, "r_frame_rate");

        String container = normalizeContainer(value(formatOutput, "format_name"));

        Double duration = doubleValue(formatOutput, "duration");

        return new MediaProbeResult(
                input,
                videoCodec,
                audioCodec,
                container,
                Files.size(input),
                duration,
                width,
                height,
                bitrate,
                frameRate
        );
    }

    private String probeAudioCodec(Path input) throws IOException {
        List<String> command = List.of(
                FFPROBE,
                "-v", "error",
                "-select_streams", "a:0",
                "-show_entries", "stream=codec_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                input.toString()
        );

        ProcessResult result = runProcess(command);

        String codec = result.stdout().trim();

        return codec.isBlank() ? null : codec;
    }

    private EncodingPlan determinePlan(MediaProbeResult probe) {

        boolean supportedVideo =  SUPPORTED_VIDEO_CODEC_H264.equalsIgnoreCase(probe.videoCodec())
                        || SUPPORTED_VIDEO_CODEC_HEVC.equalsIgnoreCase( probe.videoCodec());

        boolean supportedAudio =  SUPPORTED_AUDIO_CODEC_AAC.equalsIgnoreCase(probe.audioCodec());

        boolean mp4 = "mp4".equalsIgnoreCase(probe.container());

        if (supportedVideo && supportedAudio && mp4) {
            return EncodingPlan.COPY;
        }

        if (supportedVideo && supportedAudio) {
            return EncodingPlan.REMUX;
        }

        if (!supportedVideo && supportedAudio) {
            return EncodingPlan.TRANSCODE_VIDEO;
        }

        if (supportedVideo) {
            return EncodingPlan.TRANSCODE_AUDIO;
        }

        return EncodingPlan.TRANSCODE_BOTH;
    }

    private Path temporaryOutput(Path input) {

        String filename = input.getFileName().toString();

        int extensionIndex = filename.lastIndexOf('.');

        String baseName = extensionIndex > 0 ? filename.substring(0, extensionIndex) : filename;

        return input.resolveSibling(baseName + "-temp.mp4");
    }

    private EncodedMediaFile replaceOriginal(Path original,Path temporary) throws IOException {
        try {
            if (!Files.exists(temporary)) {
                throw new IOException( "FFmpeg completed but output file does not exist: "+ temporary);
            }

            Files.delete(original);
            Path finalPath = original.resolveSibling(removeExtension(original.getFileName().toString()) + ".mp4" );
            Files.move(temporary, finalPath);

            log.info("Encoding successful: {} -> {}",original, finalPath);

            /*
             * probe actual result instead of assuming setting were applied correctly
             */
            return EncodedMediaFile.from(probe(finalPath), extractTmdbId(finalPath));

        } catch (Exception e) {
            Files.deleteIfExists(temporary);
            throw e;
        }
    }

    private String removeExtension(String filename) {
        int index = filename.lastIndexOf('.');

        return index > 0
                ? filename.substring(0, index)
                : filename;
    }

    private String normalizeContainer(String container) {
        if (container == null) {
            return null;
        }

        if (container.contains("mp4")) {
            return "mp4";
        }

        return container;
    }

    private String value(String output, String key) {
        for (String line : output.split("\\R")) {
            if (line.startsWith(key + "=")) {
                return line.substring(key.length() + 1).trim();
            }
        }

        return null;
    }

    private Integer integerValue(String output, String key) {
        String value = value(output, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long longValue(String output, String key) {
        String value = value(output, key);

        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double doubleValue(String output, String key) {
        String value = value(output, key);

        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ProcessResult runProcess(List<String> command)
            throws IOException {
        log.debug("Running command: {}", command);

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

        String output;

        try (BufferedReader reader =new BufferedReader( new InputStreamReader(process.getInputStream()))) {

            output = reader.lines().reduce("", (a, b) -> a + b + "\n");
        }

        try {
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("Command failed with exit code "+ exitCode+ ": "+ String.join(" ", command)+ "\n"+ output);
            }

            return new ProcessResult( output.trim(),"");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IOException("Process interrupted: "+ String.join(" ", command),e);
        }
    }

    private record ProcessResult(
            String stdout,
            String stderr
    ) {
    }

    private enum EncodingPlan {
        COPY,
        REMUX,
        TRANSCODE_VIDEO,
        TRANSCODE_AUDIO,
        TRANSCODE_BOTH
    }
}