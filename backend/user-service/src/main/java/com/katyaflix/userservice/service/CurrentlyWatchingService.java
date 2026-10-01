package com.katyaflix.userservice.service;

import com.katyaflix.userservice.dto.EpisodeDto;
import com.katyaflix.userservice.dto.MediaDto;

import com.katyaflix.userservice.dto.MediaType;
import com.katyaflix.userservice.entity.User;
import com.katyaflix.userservice.entity.Watchtime;
import com.katyaflix.userservice.repository.UserRepository;
import com.katyaflix.userservice.repository.WatchtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CurrentlyWatchingService {
    private final WatchtimeRepository watchtimeRepository;
    private final UserRepository userRepository;

    @Value("${media.url}")
    private String mediaUrlRoot;


    public List<MediaDto> getCurrentlyWatching(UUID userId) {

        List<Watchtime> userWatchtimes = watchtimeRepository.findByUser(getUser(userId));
        List<MediaDto> currentlyWatching = new ArrayList<>();
        Set<UUID> addedMediaIds = new HashSet<>();

        for (Watchtime w : userWatchtimes) {

            UUID mediaId;
            MediaType mediaType;

            if (w.getMovieId() != null) {
                mediaId = w.getMovieId();
                mediaType = MediaType.MOVIE;
            } else {
                mediaId = w.getShowId();
                mediaType = MediaType.SHOW;
            }

            if (addedMediaIds.add(mediaId)) {
                currentlyWatching.add(new MediaDto(mediaId, mediaType));
            }
        }

        return currentlyWatching;
    }
    public EpisodeDto getCurrentEpisode(UUID userId, UUID showId) {
        Watchtime recentWatchtime = watchtimeRepository.findFirstByUserAndShowIdOrderByUpdatedAtDesc(getUser(userId), showId);

        return new EpisodeDto(recentWatchtime.getEpisodeId(), recentWatchtime.getSeasonId(), recentWatchtime.getShowId(), recentWatchtime.getWatchtimeSeconds(), recentWatchtime.getDurationSeconds());
    }
    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found: " + userId
                        )
                );
    }


}