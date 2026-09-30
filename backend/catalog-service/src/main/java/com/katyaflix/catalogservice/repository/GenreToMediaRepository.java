package com.katyaflix.catalogservice.repository;

import com.katyaflix.catalogservice.entity.GenreToMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GenreToMediaRepository extends JpaRepository<GenreToMedia, UUID> {
}