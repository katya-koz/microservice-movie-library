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
    @JoinColumn(name = "media_file_id", nullable = false)
    private MediaFile mediaFile;

    // Location of the .vtt sidecar file
    @Column(name = "file_path", nullable = false, unique = true)
    private String filePath;

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

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}