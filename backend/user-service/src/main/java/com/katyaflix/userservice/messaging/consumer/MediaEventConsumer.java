//package com.katyaflix.userservice.messaging.consumer;
//
//import com.katyaflix.userservice.messaging.event.MediaDeletedEvent;
//import com.katyaflix.userservice.repository.WatchlistRepository;
//import com.katyaflix.userservice.repository.WatchtimeRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//import tools.jackson.databind.ObjectMapper;
//
///**
// * Keeps watchlists/watchtimes consistent with the media-service's catalog:
// * when a title is deleted there, we drop any references to it here rather
// * than leaving orphaned rows a client would have to defensively filter out.
// */
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class MediaEventConsumer {
//
//    private final WatchlistRepository watchlistRepository;
//    private final WatchtimeRepository watchtimeRepository;
//    private final ObjectMapper objectMapper;
//
//    @KafkaListener(topics = "${app.kafka.topics.media-deleted-events}")
//    @Transactional
//    public void onMediaDeleted(String payload) {
//        MediaDeletedEvent event;
//        try {
//            event = objectMapper.readValue(payload, MediaDeletedEvent.class);
//        } catch (Exception e) {
//            log.warn("Could not parse media-deleted event, skipping: {}", payload, e);
//            return;
//        }
//
//        log.info("Received media-deleted event: {}", event);
//        switch (event.mediaType()) {
//            case MOVIE -> {
//                watchlistRepository.deleteByMovieId(event.mediaId());
//                watchtimeRepository.deleteByMovieId(event.mediaId());
//            }
//            case SHOW -> watchlistRepository.deleteByShowId(event.mediaId());
//            case EPISODE -> watchtimeRepository.deleteByEpisodeId(event.mediaId());
//        }
//    }
//}
