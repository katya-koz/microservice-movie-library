//package com.katyaflix.userservice.messaging.event;
//
//import com.katyaflix.useractivityservice.dto.MediaType;
//
//import java.util.UUID;
//
///**
// * Consumed from app.kafka.topics.media-deleted-events. Produced upstream by
// * the media-service when a movie/show/episode is deleted, so we can cascade
// * cleanup of watchlist and watchtime rows that reference it.
// */
//public record MediaDeletedEvent(
//        MediaType mediaType,
//        UUID mediaId
//) {
//}
