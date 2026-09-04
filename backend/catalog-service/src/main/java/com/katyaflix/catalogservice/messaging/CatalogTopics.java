package com.katyaflix.catalogservice.messaging;

public final class CatalogTopics {
    private CatalogTopics() {}

    public static final String MOVIE_VALIDATION_REQUESTED = "catalog.movie.validation.requested";
    public static final String SHOW_VALIDATION_REQUESTED = "catalog.show.validation.requested";
    public static final String MOVIE_VALIDATION_COMPLETED = "catalog.movie.validation.completed";
    public static final String SHOW_VALIDATION_COMPLETED = "catalog.show.validation.completed";
    public static final String MEDIA_ENRICHMENT_REQUESTED = "catalog.media.enrichment.requested";
    public static final String MEDIA_ENRICHMENT_COMPLETED = "catalog.media.enrichment.completed";
    public static final String FILE_PATH_UPDATE_REQUESTED = "catalog.media.file-path-update.requested";
    public static final String FILE_PATH_UPDATE_COMPLETED = "catalog.media.file-path-update.completed";
}
