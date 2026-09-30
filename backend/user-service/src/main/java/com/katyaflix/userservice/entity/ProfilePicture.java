package com.katyaflix.userservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A selectable avatar image. Rows are seeded ahead of time (see db/init.sql
 * for the table); this service only reads/references them.
 */
@Entity
@Table(name = "profile_pictures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilePicture {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    /** Path relative to the media store root, e.g. "/pfp/astronaut.png". */
    private String location;
}
