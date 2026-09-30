package com.katyaflix.userservice.dto;

import java.util.UUID;

public record ProfileDto(
        UUID id,
        String displayName,
        UUID profilePictureId,
        String profilePictureUrl
) {
}
