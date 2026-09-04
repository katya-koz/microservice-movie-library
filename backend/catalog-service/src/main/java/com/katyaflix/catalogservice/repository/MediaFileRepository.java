package com.katyaflix.catalogservice.repository;

import com.katyaflix.catalogservice.entity.MediaFile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    Optional<MediaFile> findByMovieId(UUID movieId);
    Optional<MediaFile> findByEpisodeId(UUID episodeId);



    @Modifying
    @Query("""
    UPDATE MediaFile e
    SET e.filePath = :filePath
    WHERE e.movie.id = :id
       OR e.episode.id = :id
""")
    int updateFilePathByMovieOrEpisodeId(
            @Param("id") UUID id,
            @Param("filePath") String filePath
    );

}