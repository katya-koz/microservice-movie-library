package com.katyaflix.userservice.dto;

import java.util.UUID;

public record ProfilePictureDto(
        UUID id,
        String name,
        String url
) {
}
