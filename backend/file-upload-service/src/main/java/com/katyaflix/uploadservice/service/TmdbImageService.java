package com.katyaflix.uploadservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * tmdb's image cdn serves their images publicly without any need for an api key
 */
@Service
public class TmdbImageService {

    private static final Logger log = LoggerFactory.getLogger(TmdbImageService.class);
    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/original";

    private final HttpClient client = HttpClient.newHttpClient();
    private static final int MAX_DOWNLOAD_ATTEMPTS = 4;
    private static final long INITIAL_RETRY_DELAY_MS = 500;

    public String download(String tmdbRelativePath, Path dest, Path mediaRoot) {
        if (tmdbRelativePath == null || tmdbRelativePath.isBlank()) {
            return null;
        }

        try {
            Files.createDirectories(dest.getParent());

            // overwrite existing image
            Files.deleteIfExists(dest);

            URI uri = URI.create(IMAGE_BASE + tmdbRelativePath);

            // retry logic might not be necessary anymore after updating docker dns
            for (int attempt = 1; attempt <= MAX_DOWNLOAD_ATTEMPTS; attempt++) {
                try {
                    HttpRequest request = HttpRequest.newBuilder(uri).GET().build();

                    HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(dest));

                    if (response.statusCode() == 200) {
                        return mediaRoot.relativize(dest).toString().replace('\\', '/');
                    }

                    log.warn("TMDB image download returned HTTP {} for {}", response.statusCode(), tmdbRelativePath);

                    return null;

                } catch (IOException e) {
                    if (attempt == MAX_DOWNLOAD_ATTEMPTS) {
                        throw e;
                    }

                    long delay = INITIAL_RETRY_DELAY_MS * (1L << (attempt - 1));

                    log.warn("TMDB image download failed for {} " + "(attempt {}/{}). Retrying in {} ms...", tmdbRelativePath, attempt, MAX_DOWNLOAD_ATTEMPTS, delay, e);

                    Thread.sleep(delay);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            log.warn("TMDB image download interrupted for {}", tmdbRelativePath, e);

        } catch (IOException e) {
            log.warn("Failed to download TMDB image {} to {} after {} attempts", tmdbRelativePath, dest, MAX_DOWNLOAD_ATTEMPTS, e);
        }

        return null;

    }
}
