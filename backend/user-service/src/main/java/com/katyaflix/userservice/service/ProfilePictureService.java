package com.katyaflix.userservice.service;

import com.katyaflix.userservice.dto.ProfilePictureDto;
import com.katyaflix.userservice.repository.ProfilePictureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Backs the profile picture carousel in the profile editor. */
@Service
@RequiredArgsConstructor
public class ProfilePictureService {

    private final ProfilePictureRepository profilePictureRepository;

    @Value("${media.url}")
    private String mediaUrlRoot;

    @Transactional(readOnly = true)
    public List<ProfilePictureDto> getAllPictures() {
        return profilePictureRepository.findAll().stream()
                .map(p -> new ProfilePictureDto(p.getId(), p.getName(), mediaUrlRoot + p.getLocation()))
                .toList();
    }
}
