package com.katyaflix.userservice.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Partial update — null fields are left unchanged. */
public record UpdateProfileRequest(
        @Size(max = 100, message = "displayName must be 100 characters or fewer")
        String displayName,

        UUID profilePictureId
) {
}
