package com.katyaflix.uploadservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled, used by EncodingQueueService to poll the mongo-backed
 * encoding queue. Safe to merge into your main @SpringBootApplication class
 * instead if you'd rather keep @EnableScheduling there - this is just a
 * separate file so it doesn't collide with a main class we don't have.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}