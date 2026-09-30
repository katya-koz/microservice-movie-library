package com.katyaflix.userservice.dto;

/**
 * MOVIE and SHOW apply to watchlists; MOVIE and EPISODE apply to watchtimes.
 * Services validate the applicable subset for each use — see
 * WatchlistService / WatchtimeService.
 */
public enum MediaType {
    MOVIE,
    SHOW,
    EPISODE
}
