package com.katyaflix.uploadservice.dto;

public record EncodedMediaFile(
        String tmdbId,
        long sizeBytes,
        Double durationSeconds,
        Integer width,
        Integer height,
        String videoCodec,
        String audioCodec,
        String container,
        Long bitrate,
        String frameRate
) {
    public static EncodedMediaFile from(
            MediaProbeResult probe,
            String tmdbId
    ) {
        return new EncodedMediaFile(
                tmdbId,
                probe.sizeBytes(),
                probe.durationSeconds(),
                probe.width(),
                probe.height(),
                probe.videoCodec(),
                probe.audioCodec(),
                probe.container(),
                probe.bitrate(),
                probe.frameRate()
        );
    }
}
