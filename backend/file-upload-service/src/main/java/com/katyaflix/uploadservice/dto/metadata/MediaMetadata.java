package com.katyaflix.uploadservice.dto.metadata;

import com.katyaflix.uploadservice.dto.enums.MediaCategory;

import java.util.List;
import java.util.UUID;

public interface MediaMetadata {

    long tmdbId();
    UUID catalogId();
    String creatorNames();
    String title();
    String overview();
    MediaCategory type();
    String posterPath();
    String backdropPath();
    List<String> genres();
}