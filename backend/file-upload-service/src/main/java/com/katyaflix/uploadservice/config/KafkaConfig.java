package com.katyaflix.uploadservice.config;

import com.katyaflix.uploadservice.dto.CatalogEventDtos;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;

    // ---- producer: publishes MovieCatalogValidationEvent / ShowCatalogValidationEvent requests ----

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        config.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ---- consumer: catalog.movie.validation.completed -> always CatalogMovieUploadEvent ----

    @Bean
    public ConsumerFactory<String, CatalogEventDtos.CatalogMovieUploadEvent> movieResultConsumerFactory() {
        JacksonJsonDeserializer<CatalogEventDtos.CatalogMovieUploadEvent> deserializer =
                new JacksonJsonDeserializer<>(CatalogEventDtos.CatalogMovieUploadEvent.class)
                        .ignoreTypeHeaders();
        return new DefaultKafkaConsumerFactory<>(baseConsumerConfig(), new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.CatalogMovieUploadEvent> movieResultContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.CatalogMovieUploadEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(movieResultConsumerFactory());
        return factory;
    }

    // ---- consumer: catalog.show.validation.completed -> always CatalogShowUploadEvent ----

    @Bean
    public ConsumerFactory<String, CatalogEventDtos.CatalogShowUploadEvent> showResultConsumerFactory() {
        JacksonJsonDeserializer<CatalogEventDtos.CatalogShowUploadEvent> deserializer =
                new JacksonJsonDeserializer<>(CatalogEventDtos.CatalogShowUploadEvent.class)
                        .ignoreTypeHeaders();
        return new DefaultKafkaConsumerFactory<>(baseConsumerConfig(), new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.CatalogShowUploadEvent> showResultContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.CatalogShowUploadEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(showResultConsumerFactory());
        return factory;
    }

    private Map<String, Object> baseConsumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return config;
    }

    // ---- consumer: catalog.media.enrichment.completed -> MediaCatalogEnrichmentEvent ----

    @Bean
    public ConsumerFactory<String, CatalogEventDtos.MediaCatalogEnrichmentEvent>
    mediaEnrichmentConsumerFactory() {

        JacksonJsonDeserializer<CatalogEventDtos.MediaCatalogEnrichmentEvent> deserializer =
                new JacksonJsonDeserializer<>(
                        CatalogEventDtos.MediaCatalogEnrichmentEvent.class
                ).ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerConfig(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.MediaCatalogEnrichmentEvent>
    mediaEnrichmentContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.MediaCatalogEnrichmentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(mediaEnrichmentConsumerFactory());

        return factory;
    }


// ---- consumer: file.path.update.completed -> FilePathUpdateEvent ----

    @Bean
    public ConsumerFactory<String, CatalogEventDtos.FilePathUpdateEvent>
    filePathUpdateConsumerFactory() {

        JacksonJsonDeserializer<CatalogEventDtos.FilePathUpdateEvent> deserializer =
                new JacksonJsonDeserializer<>(
                        CatalogEventDtos.FilePathUpdateEvent.class
                ).ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerConfig(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.FilePathUpdateEvent>
    filePathUpdateContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.FilePathUpdateEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(filePathUpdateConsumerFactory());

        return factory;
    }
}
