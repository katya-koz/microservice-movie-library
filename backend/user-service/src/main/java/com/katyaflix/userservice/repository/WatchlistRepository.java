package com.katyaflix.userservice.repository;

import com.katyaflix.userservice.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {

    List<Watchlist> findByUserId(UUID userId);

    Optional<Watchlist> findByUserIdAndMovieId(UUID userId, UUID movieId);

    Optional<Watchlist> findByUserIdAndShowId(UUID userId, UUID showId);

    boolean existsByUserIdAndMovieId(UUID userId, UUID movieId);

    boolean existsByUserIdAndShowId(UUID userId, UUID showId);

    // Cascade cleanup when the media-service tells us a title was deleted.
    void deleteByMovieId(UUID movieId);

    void deleteByShowId(UUID showId);
}
