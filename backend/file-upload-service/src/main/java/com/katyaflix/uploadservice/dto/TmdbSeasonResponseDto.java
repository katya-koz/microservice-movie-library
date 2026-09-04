package com.katyaflix.uploadservice.dto;

import java.util.List;

public record TmdbSeasonResponseDto(List<TmdbSeasonEpisodeDto> episodes) {}
