

package com.katyaflix.catalogservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class TmdbEntity {

    @Column(name = "tmdb_id")
    protected long tmdbId;
}