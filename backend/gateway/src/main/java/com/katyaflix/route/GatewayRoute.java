package com.katyaflix.gateway.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@Configuration
public class GatewayRoute {

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()

				.route("catalog_movies_route", r -> r
						.path("/api/movies", "/api/movies/**")
						.filters(f -> f.stripPrefix(1))
						.uri("http://catalog-service:8080")
				)

				.route("catalog_shows_route", r -> r
						.path("/api/shows", "/api/shows/**")
						.filters(f -> f.stripPrefix(1))
						.uri("http://catalog-service:8080")
				)

				.route("catalog_playback_route", r -> r
						.path("/api/playback", "/api/playback/**")
						.filters(f -> f.stripPrefix(1))
						.uri("http://catalog-service:8080")
				)

				.route("upload_route", r -> r
						.path("/api/uploads", "/api/uploads/**")
						.filters(f -> f.stripPrefix(1))
						.uri("http://file-upload-service:8080")
				)

				.route("tmdb_route", r -> r
						.path("/api/tmdb", "/api/tmdb/**")
						.filters(f -> f.stripPrefix(1))
						.uri("http://file-upload-service:8080")
				)

				.build();
	}
}