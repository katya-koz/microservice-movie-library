package com.katyaflix.catalogservice.config;

import com.katyaflix.catalogservice.dto.CatalogEventDtos;
import com.katyaflix.catalogservice.dto.CatalogEventDtos.MovieCatalogValidationEvent;
import com.katyaflix.catalogservice.dto.CatalogEventDtos.ShowCatalogValidationEvent;
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

    // ---- producer: publishes CatalogMovieUploadEvent / CatalogShowUploadEvent results ----

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

    // ---- consumer: catalog.movie.validation.requested -> always MovieCatalogValidationEvent ----

    @Bean
    public ConsumerFactory<String, MovieCatalogValidationEvent> movieValidationConsumerFactory() {
        JacksonJsonDeserializer<MovieCatalogValidationEvent> deserializer =
                new JacksonJsonDeserializer<>(MovieCatalogValidationEvent.class)
                        .ignoreTypeHeaders();
        return new DefaultKafkaConsumerFactory<>(baseConsumerConfig(), new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MovieCatalogValidationEvent> movieValidationContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MovieCatalogValidationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(movieValidationConsumerFactory());
        return factory;
    }

    // ---- consumer: catalog.show.validation.requested -> always ShowCatalogValidationEvent ----

    @Bean
    public ConsumerFactory<String, ShowCatalogValidationEvent> showValidationConsumerFactory() {
        JacksonJsonDeserializer<ShowCatalogValidationEvent> deserializer =
                new JacksonJsonDeserializer<>(ShowCatalogValidationEvent.class)
                        .ignoreTypeHeaders();
        return new DefaultKafkaConsumerFactory<>(baseConsumerConfig(), new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ShowCatalogValidationEvent> showValidationContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ShowCatalogValidationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(showValidationConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, CatalogEventDtos.MediaFileEnrichmentEvent> mediaEnrichmentConsumerFactory() {
        JacksonJsonDeserializer<CatalogEventDtos.MediaFileEnrichmentEvent> deserializer =
                new JacksonJsonDeserializer<>(CatalogEventDtos.MediaFileEnrichmentEvent.class)
                        .ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerConfig(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.MediaFileEnrichmentEvent>
    mediaEnrichmentContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.MediaFileEnrichmentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(mediaEnrichmentConsumerFactory());

        return factory;
    }


// ---- consumer: file path update requested -> always CatalogPathUpdateEvent ----

    @Bean
    public ConsumerFactory<String, CatalogEventDtos.CatalogPathUpdateEvent> filePathUpdateConsumerFactory() {
        JacksonJsonDeserializer<CatalogEventDtos.CatalogPathUpdateEvent> deserializer =
                new JacksonJsonDeserializer<>(CatalogEventDtos.CatalogPathUpdateEvent.class)
                        .ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerConfig(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.CatalogPathUpdateEvent>
    filePathUpdateContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, CatalogEventDtos.CatalogPathUpdateEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(filePathUpdateConsumerFactory());

        return factory;
    }

    private Map<String, Object> baseConsumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return config;
    }
}


