package com.katyaflix.catalogservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subtitles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subtitle {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = true)
    private Episode episode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = true)
    private Movie movie;

    // Location of the .vtt sidecar file
    @Column(name = "file_path", nullable = false, unique = true)
    private String filePath;

    @Column(name = "original_file_name", nullable = false, unique = true)
    private String originalFileName;

    @Column(name = "language_code", nullable = false)
    private String languageCode;

    private String label;

    @Builder.Default
    private String format = "vtt";

    @Column(name = "is_forced")
    @Builder.Default
    private boolean isForced = false;

    @Column(name = "is_sdh")
    @Builder.Default
    private boolean isSdh = false;

    @Column(name = "is_default")
    @Builder.Default
    private boolean isDefault = false;

    // embedded, external, downloaded
    @Builder.Default
    private String source = "embedded";

    @Column(name = "original_stream_index")
    private Integer originalStreamIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private void validateExactlyOneOwner() {
        boolean hasMovie = movie != null;
        boolean hasEpisode = episode != null;
        if (hasMovie == hasEpisode) { // both set, or both unset
            throw new IllegalStateException(
                    "Subtitle must belong to exactly one of movie or episode");
        }
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}