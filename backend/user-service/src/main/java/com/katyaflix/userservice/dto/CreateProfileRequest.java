package com.katyaflix.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProfileRequest(
        @NotBlank(message = "displayName is required")
        @Size(max = 100, message = "displayName must be 100 characters or fewer")
        String displayName,

        // Optional — a default can be assigned later via update.
        UUID profilePictureId
) {
}
