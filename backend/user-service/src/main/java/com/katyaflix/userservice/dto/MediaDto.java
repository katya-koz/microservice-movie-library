package com.katyaflix.userservice.dto;

import java.util.UUID;

public record MediaDto(
        UUID mediaId,
        MediaType mediaType
   ){
}
