package com.katyaflix.userservice.controller;

import com.katyaflix.userservice.dto.ProfilePictureDto;
import com.katyaflix.userservice.service.ProfilePictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** All selectable avatar images, for the profile picture editor carousel. */
@RestController
@RequestMapping("/profile-pictures")
@RequiredArgsConstructor
public class ProfilePictureController {

    private final ProfilePictureService profilePictureService;

    @GetMapping
    public List<ProfilePictureDto> getAllPictures() {
        return profilePictureService.getAllPictures();
    }
}
