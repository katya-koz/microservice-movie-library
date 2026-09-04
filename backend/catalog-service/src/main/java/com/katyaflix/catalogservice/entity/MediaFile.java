package com.katyaflix.catalogservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id")
    private Episode episode;

    @Column(name = "file_path", unique = true)
    private String filePath;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "container_format")
    private String containerFormat;

    @Column(name = "video_codec")
    private String videoCodec;

    @Column(name = "audio_codec")
    private String audioCodec;

    private String resolution;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "encoded_at")
    private Instant encodedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        validateExactlyOneOwner();
    }

    @PreUpdate
    void onUpdate() {
        validateExactlyOneOwner();
    }

    private void validateExactlyOneOwner() {
        boolean hasMovie = movie != null;
        boolean hasEpisode = episode != null;
        if (hasMovie == hasEpisode) { // both set, or both unset
            throw new IllegalStateException(
                    "MediaFile must belong to exactly one of movie or episode");
        }
    }
}