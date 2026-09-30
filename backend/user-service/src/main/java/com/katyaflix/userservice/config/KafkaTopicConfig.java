package com.katyaflix.userservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the topics this service owns/publishes to, so they exist with
 * sane defaults even against a fresh broker in local/dev environments.
 * app.kafka.topics.media-deleted-events is intentionally NOT declared here —
 * it's owned and created by the media-service, we only consume it.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic watchtimeEventsTopic(@Value("${app.kafka.topics.watchtime-events}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic watchlistEventsTopic(@Value("${app.kafka.topics.watchlist-events}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic profileEventsTopic(@Value("${app.kafka.topics.profile-events}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
