package com.katyaflix.userservice.repository;

import com.katyaflix.userservice.entity.User;
import com.katyaflix.userservice.entity.Watchtime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchtimeRepository extends JpaRepository<Watchtime, UUID> {

    List<Watchtime> findByUser(User user);

    void deleteByMovieId(UUID movieId);

    void deleteByEpisodeId(UUID episodeId);
    

    Optional<Watchtime> findByUserAndMovieId(User user, UUID mediaId);

    Optional<Watchtime> findByUserAndEpisodeId(User user, UUID mediaId);

    List<Watchtime> findByUserAndMovieIdIn(User user, List<UUID> mediaIds);

    List<Watchtime> findByUserAndEpisodeIdIn(User user, List<UUID> mediaIds);

    Watchtime findFirstByUserAndShowIdOrderByUpdatedAtDesc(User user, UUID showId);
}
