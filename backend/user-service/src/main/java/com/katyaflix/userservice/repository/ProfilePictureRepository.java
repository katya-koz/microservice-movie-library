package com.katyaflix.userservice.repository;

import com.katyaflix.userservice.entity.ProfilePicture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfilePictureRepository extends JpaRepository<ProfilePicture, UUID> {
}
