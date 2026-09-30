package com.katyaflix.userservice.service;

import com.katyaflix.userservice.dto.MediaType;
import com.katyaflix.userservice.dto.UpdateWatchtimeRequest;
import com.katyaflix.userservice.dto.WatchtimeDto;
import com.katyaflix.userservice.entity.User;
import com.katyaflix.userservice.entity.Watchtime;
import com.katyaflix.userservice.messaging.event.WatchtimeUpdatedEvent;
import com.katyaflix.userservice.messaging.producer.UserEventProducer;
import com.katyaflix.userservice.repository.UserRepository;
import com.katyaflix.userservice.repository.WatchtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchtimeService {

    private final WatchtimeRepository watchtimeRepository;
    private final UserRepository userRepository;
    private final UserEventProducer eventProducer;

    @Transactional(readOnly = true)
    public List<WatchtimeDto> getAllWatchtimes(UUID userId) {
        User user = getUser(userId);

        return watchtimeRepository.findByUser(user)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<WatchtimeDto> getWatchtime(
            UUID userId,
            MediaType mediaType,
            UUID mediaId
    ) {
        User user = getUser(userId);

        return findEntity(user, mediaType, mediaId)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<WatchtimeDto> getWatchtimes(
            UUID userId,
            MediaType mediaType,
            List<UUID> mediaIds
    ) {
        User user = getUser(userId);

        List<Watchtime> results;

        if (mediaType == MediaType.MOVIE) {
            results = watchtimeRepository.findByUserAndMovieIdIn(user,mediaIds );
        } else if (mediaType == MediaType.EPISODE) {
            results = watchtimeRepository.findByUserAndEpisodeIdIn(user,mediaIds);
        } else {
            throw new IllegalArgumentException(
                    "Watchtime only applies to movies or episodes"
            );
        }

        Map<UUID, Watchtime> watchtimes = results.stream().collect(Collectors.toMap(
                watchtime -> mediaType == MediaType.MOVIE ? watchtime.getMovieId() : watchtime.getEpisodeId(),
                Function.identity()
                ));

        return mediaIds.stream() .map(mediaId -> {
                    Watchtime watchtime = watchtimes.get(mediaId);
                    if (watchtime == null) {
                        return new WatchtimeDto(
                                mediaType,
                                mediaId,
                                0,
                                null,
                                0

                        );
                    }

                    return new WatchtimeDto(
                            mediaType,
                            mediaId,
                            watchtime.getWatchtimeSeconds(),
                            watchtime.getUpdatedAt(),
                            watchtime.getDurationSeconds()
                    );
                })
                .toList();
    }

    @Transactional
    public WatchtimeDto upsertWatchtime(  UUID userId, UpdateWatchtimeRequest request ) {
        User user = getUser(userId);

        if (request.mediaType() == MediaType.SHOW) {
            throw new IllegalArgumentException(   "Watchtime applies to MOVIE or EPISODE only"  );
        }

        Watchtime watchtime = findEntity( user, request.mediaType(), request.mediaId()
        ).orElseGet(() ->  createWatchtime( user, request.mediaType(),  request.mediaId()  ) );

        watchtime.setWatchtimeSeconds(request.watchtimeSeconds());
        watchtime.setDurationSeconds(request.durationSeconds());
        watchtime.setShowId(request.showId());
        watchtime.setSeasonId(request.seasonId());
        Watchtime saved = watchtimeRepository.save(watchtime);

//        eventProducer.publishWatchtimeUpdated(
//                new WatchtimeUpdatedEvent(
//                        user.getId(),
//                        request.mediaType(),
//                        request.mediaId(),
//                        saved.getWatchtimeSeconds(),
//                        saved.getUpdatedAt()
//                )
//        );

        return toDto(saved);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found: " + userId
                        )
                );
    }

    private Watchtime createWatchtime(
            User user,
            MediaType mediaType,
            UUID mediaId
    ) {
        Watchtime.WatchtimeBuilder builder = Watchtime.builder()
                .user(user)
                .watchtimeSeconds(0);

        if (mediaType == MediaType.MOVIE) {
            builder.movieId(mediaId);
        } else if (mediaType == MediaType.EPISODE) {
            builder.episodeId(mediaId);
        } else {
            throw new IllegalArgumentException(
                    "Watchtime only applies to movies or episodes"
            );
        }

        return builder.build();
    }

    private Optional<Watchtime> findEntity(
            User user,
            MediaType mediaType,
            UUID mediaId
    ) {
        if (mediaType == MediaType.MOVIE) {
            return watchtimeRepository.findByUserAndMovieId(
                    user,
                    mediaId
            );
        }

        if (mediaType == MediaType.EPISODE) {
            return watchtimeRepository.findByUserAndEpisodeId(
                    user,
                    mediaId
            );
        }

        throw new IllegalArgumentException(
                "Watchtime only applies to movies or episodes"
        );
    }

    private WatchtimeDto toDto(Watchtime watchtime) {
        if (watchtime.getMovieId() != null) {
            return new WatchtimeDto(
                    MediaType.MOVIE,
                    watchtime.getMovieId(),
                    watchtime.getWatchtimeSeconds(),
                    watchtime.getUpdatedAt(),
                    watchtime.getDurationSeconds()
            );
        }

        return new WatchtimeDto(
                MediaType.EPISODE,
                watchtime.getEpisodeId(),
                watchtime.getWatchtimeSeconds(),
                watchtime.getUpdatedAt(),
                watchtime.getDurationSeconds()
        );
    }
}