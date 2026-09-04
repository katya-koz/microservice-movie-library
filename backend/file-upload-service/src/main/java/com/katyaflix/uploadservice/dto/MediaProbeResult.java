package com.katyaflix.uploadservice.dto;


import java.nio.file.Path;

public record MediaProbeResult(
        Path path,
        String videoCodec,
        String audioCodec,
        String container,
        long sizeBytes,
        Double durationSeconds,
        Integer width,
        Integer height,
        Long bitrate,
        String frameRate
) {
}