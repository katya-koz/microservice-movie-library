package com.katyaflix.userservice.controller;

import com.katyaflix.userservice.dto.CreateProfileRequest;
import com.katyaflix.userservice.dto.ProfileDto;
import com.katyaflix.userservice.dto.UpdateProfileRequest;
import com.katyaflix.userservice.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Profile selection + management — "see all profiles", add/remove/update. */
@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public List<ProfileDto> getAllProfiles() {
        return profileService.getAllProfiles();
    }

    @GetMapping("/{id}")
    public ProfileDto getProfile(@PathVariable UUID id) {
        return profileService.getProfile(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileDto createProfile(@Valid @RequestBody CreateProfileRequest request) {
        return profileService.createProfile(request);
    }

    @PatchMapping("/{id}")
    public ProfileDto updateProfile(@PathVariable UUID id, @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable UUID id) {
        profileService.deleteProfile(id);
    }
}
