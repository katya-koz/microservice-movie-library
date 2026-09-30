package com.katyaflix.userservice.service;

import com.katyaflix.userservice.dto.CreateProfileRequest;
import com.katyaflix.userservice.dto.ProfileDto;
import com.katyaflix.userservice.dto.UpdateProfileRequest;
import com.katyaflix.userservice.entity.ProfilePicture;
import com.katyaflix.userservice.entity.User;
import com.katyaflix.userservice.exception.ResourceNotFoundException;
import com.katyaflix.userservice.messaging.event.ProfileEvent;
import com.katyaflix.userservice.messaging.producer.UserEventProducer;
import com.katyaflix.userservice.repository.ProfilePictureRepository;
import com.katyaflix.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final ProfilePictureRepository profilePictureRepository;
    private final UserEventProducer eventProducer;

    @Value("${media.url}")
    private String mediaUrlRoot;

    @Transactional(readOnly = true)
    public List<ProfileDto> getAllProfiles() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProfileDto getProfile(UUID id) {
        return toDto(getUserOrThrow(id));
    }

    @Transactional
    public ProfileDto createProfile(CreateProfileRequest request) {
        ProfilePicture picture = resolvePicture(request.profilePictureId());

        User user = User.builder()
                .displayName(request.displayName().trim())
                .profilePicture(picture)
                .build();
        user = userRepository.save(user);

        eventProducer.publishProfileEvent(
                new ProfileEvent(user.getId(), user.getDisplayName(), ProfileEvent.ChangeType.CREATED));

        return toDto(user);
    }

    @Transactional
    public ProfileDto updateProfile(UUID id, UpdateProfileRequest request) {
        User user = getUserOrThrow(id);

        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.profilePictureId() != null) {
            user.setProfilePicture(resolvePicture(request.profilePictureId()));
        }

        user = userRepository.save(user);

        eventProducer.publishProfileEvent(
                new ProfileEvent(user.getId(), user.getDisplayName(), ProfileEvent.ChangeType.UPDATED));

        return toDto(user);
    }

    @Transactional
    public void deleteProfile(UUID id) {
        User user = getUserOrThrow(id);
        userRepository.delete(user);

        eventProducer.publishProfileEvent(
                new ProfileEvent(id, user.getDisplayName(), ProfileEvent.ChangeType.DELETED));
    }

    private ProfilePicture resolvePicture(UUID pictureId) {
        if (pictureId == null) {
            return null;
        }
        return profilePictureRepository.findById(pictureId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile picture not found: " + pictureId));
    }

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + id));
    }

    private ProfileDto toDto(User user) {
        ProfilePicture picture = user.getProfilePicture();
        return new ProfileDto(
                user.getId(),
                user.getDisplayName(),
                picture != null ? picture.getId() : null,
                picture != null ? mediaUrlRoot + picture.getLocation() : null
        );
    }
}
