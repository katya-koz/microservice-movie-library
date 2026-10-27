package com.katyaflix.catalogservice.service;

import com.katyaflix.catalogservice.dto.mediaenrichment.MediaEnrichment;
import com.katyaflix.catalogservice.entity.Episode;
import com.katyaflix.catalogservice.entity.MediaFile;
import com.katyaflix.catalogservice.entity.Movie;
import com.katyaflix.catalogservice.repository.EpisodeRepository;
import com.katyaflix.catalogservice.repository.MediaFileRepository;
import com.katyaflix.catalogservice.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class MediaService {
    private final MediaFileRepository mediaFileRepository;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;

        @Transactional
        public MediaFile upsertMovieFile(MediaEnrichment mediaFileEnrichment) {

            Movie movie = movieRepository.getReferenceById(mediaFileEnrichment.id());

            MediaFile mediaFile = mediaFileRepository.findByMovieId(mediaFileEnrichment.id()).orElseGet(() -> new MediaFile());

            mediaFile.setContainerFormat(mediaFileEnrichment.containerFormat());
            mediaFile.setMovie(movie);
            mediaFile.setOriginalFilename(mediaFileEnrichment.originalFilename());
            mediaFile.setVideoCodec(mediaFileEnrichment.videoCodec());
            mediaFile.setAudioCodec(mediaFileEnrichment.audioCodec());
            mediaFile.setResolution(mediaFileEnrichment.resolution());
            mediaFile.setDurationSeconds(mediaFileEnrichment.durationSeconds());
            mediaFile.setFileSizeBytes(mediaFileEnrichment.fileSizeBytes());
            mediaFile.setEncodedAt(mediaFileEnrichment.encodedAt());
            mediaFile.setFilePath(mediaFileEnrichment.filePath());
            mediaFile.setCreatedAt(Instant.now());
            mediaFileRepository.save(mediaFile);

            return mediaFile;
        }

    @Transactional
    public MediaFile upsertEpisodeFile(MediaEnrichment mediaFileEnrichment) {

        Episode episode = episodeRepository.getReferenceById(mediaFileEnrichment.id());

        MediaFile mediaFile = mediaFileRepository.findByEpisodeId(mediaFileEnrichment.id()).orElseGet(() -> new MediaFile());

        mediaFile.setContainerFormat(mediaFileEnrichment.containerFormat());
        mediaFile.setEpisode(episode);
        mediaFile.setOriginalFilename(mediaFileEnrichment.originalFilename());
        mediaFile.setVideoCodec(mediaFileEnrichment.videoCodec());
        mediaFile.setAudioCodec(mediaFileEnrichment.audioCodec());
        mediaFile.setResolution(mediaFileEnrichment.resolution());
        mediaFile.setDurationSeconds(mediaFileEnrichment.durationSeconds());
        mediaFile.setFileSizeBytes(mediaFileEnrichment.fileSizeBytes());
        mediaFile.setEncodedAt(mediaFileEnrichment.encodedAt());
        mediaFile.setFilePath(mediaFileEnrichment.filePath());
        mediaFile.setCreatedAt(Instant.now());
        mediaFileRepository.save(mediaFile);

        return mediaFile;
    }


}
