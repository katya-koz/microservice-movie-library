package com.katyaflix.userservice.messaging.producer;

import com.katyaflix.userservice.messaging.event.ProfileEvent;
import com.katyaflix.userservice.messaging.event.WatchlistChangedEvent;
import com.katyaflix.userservice.messaging.event.WatchtimeUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around KafkaTemplate. Keyed by userId so events for the same
 * profile land on the same partition and stay ordered relative to each
 * other.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.watchtime-events}")
    private String watchtimeTopic;

    @Value("${app.kafka.topics.watchlist-events}")
    private String watchlistTopic;

    @Value("${app.kafka.topics.profile-events}")
    private String profileTopic;

    public void publishWatchtimeUpdated(WatchtimeUpdatedEvent event) {
        send(watchtimeTopic, event.userId().toString(), event);
    }

    public void publishWatchlistChanged(WatchlistChangedEvent event) {
        send(watchlistTopic, event.userId().toString(), event);
    }

    public void publishProfileEvent(ProfileEvent event) {
        send(profileTopic, event.userId().toString(), event);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                // Deliberately non-fatal: publishing failures shouldn't roll
                // back the DB write that already succeeded. Logged for
                // alerting/retry tooling to pick up.
                log.error("Failed to publish event to topic {}: {}", topic, payload, ex);
            }
        });
    }
}
