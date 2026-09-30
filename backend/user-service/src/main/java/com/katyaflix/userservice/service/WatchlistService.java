package com.katyaflix.userservice.service;

import com.katyaflix.userservice.dto.AddToWatchlistRequest;
import com.katyaflix.userservice.dto.MediaType;
import com.katyaflix.userservice.dto.WatchlistItemDto;
import com.katyaflix.userservice.entity.User;
import com.katyaflix.userservice.entity.Watchlist;
import com.katyaflix.userservice.exception.ResourceNotFoundException;
import com.katyaflix.userservice.messaging.event.WatchlistChangedEvent;
import com.katyaflix.userservice.messaging.producer.UserEventProducer;
import com.katyaflix.userservice.repository.UserRepository;
import com.katyaflix.userservice.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final UserEventProducer eventProducer;

    @Transactional(readOnly = true)
    public List<WatchlistItemDto> getWatchlist(UUID userId) {
        ensureUserExists(userId);
        return watchlistRepository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    public WatchlistItemDto addToWatchlist(UUID userId, AddToWatchlistRequest request) {
        if (request.mediaType() == MediaType.EPISODE) {
            throw new IllegalArgumentException("Only MOVIE or SHOW can be added to a watchlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + userId));

        boolean alreadyOnList = request.mediaType() == MediaType.MOVIE
                ? watchlistRepository.existsByUserIdAndMovieId(userId, request.mediaId())
                : watchlistRepository.existsByUserIdAndShowId(userId, request.mediaId());
        if (alreadyOnList) {
            throw new IllegalStateException("Already on this profile's list");
        }

        Watchlist.WatchlistBuilder builder = Watchlist.builder().user(user);
        if (request.mediaType() == MediaType.MOVIE) {
            builder.movieId(request.mediaId());
        } else {
            builder.showId(request.mediaId());
        }
        Watchlist saved = watchlistRepository.save(builder.build());

        eventProducer.publishWatchlistChanged(new WatchlistChangedEvent(
                userId, request.mediaType(), request.mediaId(), WatchlistChangedEvent.ChangeType.ADDED));

        return toDto(saved);
    }

    @Transactional
    public void removeFromWatchlist(UUID userId, MediaType mediaType, UUID mediaId) {
        if (mediaType == MediaType.EPISODE) {
            throw new IllegalArgumentException("Only MOVIE or SHOW can be removed from a watchlist");
        }

        Watchlist item = (mediaType == MediaType.MOVIE
                ? watchlistRepository.findByUserIdAndMovieId(userId, mediaId)
                : watchlistRepository.findByUserIdAndShowId(userId, mediaId))
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found"));

        watchlistRepository.delete(item);

        eventProducer.publishWatchlistChanged(new WatchlistChangedEvent(
                userId, mediaType, mediaId, WatchlistChangedEvent.ChangeType.REMOVED));
    }

    private void ensureUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Profile not found: " + userId);
        }
    }

    private WatchlistItemDto toDto(Watchlist w) {
        MediaType type = w.getMovieId() != null ? MediaType.MOVIE : MediaType.SHOW;
        UUID mediaId = w.getMovieId() != null ? w.getMovieId() : w.getShowId();
        return new WatchlistItemDto(w.getId(), type, mediaId, w.getAdded());
    }
}
