package com.katyaflix.uploadservice.service;

import com.katyaflix.uploadservice.dto.EncodedMediaFile;
import com.katyaflix.uploadservice.dto.FileUploadMetadata;
import com.katyaflix.uploadservice.entity.UploadJob;
import com.katyaflix.uploadservice.messaging.CatalogValidationProducer;
import com.katyaflix.uploadservice.repository.UploadJobRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {
    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final UploadJobRepository jobRepository;
    private final TmdbImageService tmdbImageService;
    private final Path mediaRoot;
    private final Path tempMediaRoot;
    private final FileFinalizationService fileFinalizationService;
    private final CatalogValidationProducer catalogValidationProducer;
    private final UploadJobRepository uploadJobRepository;
    private final FileEncodingService fileEncodingService;

    public UploadService(
            UploadJobRepository jobRepository,
            TmdbImageService tmdbImageService,
            @Value("${media.root}") String mediaRoot,
            FileFinalizationService fileFinalizationService,
            CatalogValidationProducer catalogValidationProducer,
            UploadJobRepository uploadJobRepository,
            FileEncodingService fileEncodingService
    ) {
        this.jobRepository = jobRepository;
        this.tmdbImageService = tmdbImageService;
        this.mediaRoot = Path.of(mediaRoot);
        this.tempMediaRoot = Path.of(mediaRoot).resolve("temp");
        this.fileFinalizationService = fileFinalizationService;
        this.catalogValidationProducer = catalogValidationProducer;

        this.uploadJobRepository = uploadJobRepository;
        this.fileEncodingService = fileEncodingService;
    }

    // create job row immedietly
    public UploadJob createJob(FileUploadMetadata.UploadMetadataDto metadata) {
        UploadJob job = new UploadJob();

        job.setMediaType(metadata.type().toString());
        job.setStatus(UploadJob.UploadStatus.PENDING);
        job.setTmdbId(metadata.tmdbId());

        // catalogPayload is  typed as the sealed UploadMetadataDto itself
        // Mongo stores movie and show metadata natively in the same collection
        if (metadata.type() == FileUploadMetadata.UploadType.MOVIE) {
            FileUploadMetadata.MovieUploadMetadataDto movie = (FileUploadMetadata.MovieUploadMetadataDto) metadata;
            job.setTitle(movie.title());
            job.setCatalogPayload(movie);

        } else {
            FileUploadMetadata.ShowUploadMetadataDto show = (FileUploadMetadata.ShowUploadMetadataDto) metadata;
            job.setTitle(show.title());
            job.setCatalogPayload(show);
        }

        return jobRepository.save(job);
    }


    public void finalizeFileUpload(UploadJob job) {
        if (!Boolean.TRUE.equals(job.getCatalogValidationStatus()) || !Boolean.TRUE.equals(job.getFileUploadStatus())) {
            throw new IllegalStateException( "finalizeFileUpload called before both sides completed for job " + job.getId());
        }

        jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.FINALIZING);
        try {

            if ("MOVIE".equalsIgnoreCase(job.getMediaType())) {
                fileFinalizationService.finalizeMovie(job);
            } else {
                fileFinalizationService.finalizeShow(job);
            }

        } catch (Exception e) {
            log.error( "File finalization failed for job {}", job.getId(), e );
            fail(job, e.getMessage());
        }
    }
    @Transactional
    public void completeFileUpload(UUID jobId) {
        jobRepository.updateStatusWhereId(jobId, UploadJob.UploadStatus.COMPLETED);
        jobRepository.updateFilePathUpdateStatusWhereId(jobId, true);
        jobRepository.updateCompletedAtWhereId(jobId);
    }

    public void processMovie(UploadJob job, FileUploadMetadata.MovieUploadMetadataDto metadata,  Map<String, MultipartFile> fileMap) {
        try {
            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.PENDING);

            // Ask catalog service to validate/upsert the movie and return its UUID.
            catalogValidationProducer.publishMovieValidation(job, metadata);
            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.SAVING_ASSETS);
            /*
             * Temporary media:
             *
             * temp/movies/{tmdbId}/{filename}
             */
            Path movieMediaDir = tempMediaRoot.resolve("movies").resolve( String.valueOf(metadata.tmdbId()));

            Map<String, Path> savedPaths = saveMovieFiles(metadata, fileMap, movieMediaDir, job);
            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.ENCODING);



            List<EncodedMediaFile> encodedFiles =  fileEncodingService.encodeAll(savedPaths);
            jobRepository.saveEncodedMediaFiles(job.getId(), encodedFiles);

            jobRepository.updateFileUploadStatusWhereId(job.getId(), true);


            advance(job.getId());

        } catch (Exception e) {
            log.error("Upload processing failed for job {}", job.getId(), e);

            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.FAILURE);
            jobRepository.appendErrorMessage(job.getId(), e.getMessage());

            throw new UploadProcessingException(  "Upload processing failed: " + e.getMessage(), e);
        }
    }

    public void advance(UUID jobId) {
        UploadJob job = uploadJobRepository.findById(jobId) .orElseThrow(() -> new IllegalStateException("Upload job not found: " + jobId));
        System.out.println(job.getFileUploadStatus() + " " + job.getCatalogValidationStatus());
        if (Boolean.TRUE.equals(job.getFileUploadStatus()) && Boolean.TRUE.equals(job.getCatalogValidationStatus())) {

            for( EncodedMediaFile encoded : job.getEncodedMediaFiles()){
                if(job.getMediaType().equals("MOVIE")){
                    FileUploadMetadata.MovieUploadMetadataDto payload = (FileUploadMetadata.MovieUploadMetadataDto) job.getCatalogPayload();

                    fileEncodingService.sendMediaEnrichmentEvent(job.getId(), payload.id(), null, "n/a", encoded);
                }else{
                    FileUploadMetadata.ShowUploadMetadataDto payload = (FileUploadMetadata.ShowUploadMetadataDto) job.getCatalogPayload();

                    fileEncodingService.sendMediaEnrichmentEvent(job.getId(), null, payload.getEpisodeByTmdbId(Long.valueOf(encoded.tmdbId())).id(), "n/a", encoded);
                }

            }
            finalizeFileUpload(job);

        } else {
            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.AWAITING_CATALOG);
        }
    }

    public void fail(UploadJob job, String message) {
        jobRepository.updateFileUploadStatusWhereId(job.getId(), false);
        jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.FAILURE);
        jobRepository.appendErrorMessage(job.getId(), message);
    }

    public void processShow(UploadJob job,
                            FileUploadMetadata.ShowUploadMetadataDto metadata,
                            Map<String, MultipartFile> fileMap) {
        try {
            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.PENDING);

            // Ask catalog service to validate/upsert the show, seasons and episodes.
            catalogValidationProducer.publishShowValidation(job, metadata);

            /*
             * Temporary media:
             *
             * temp/shows/{showTmdbId}/...
             */
            Path showMediaDir = tempMediaRoot.resolve("shows").resolve(
                    String.valueOf(metadata.tmdbId()));



            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.SAVING_ASSETS);
            Map<String, Path> savedPaths = saveShowFiles(metadata, fileMap, showMediaDir, job);

            jobRepository.updateStatusWhereId(job.getId(), UploadJob.UploadStatus.ENCODING);

            List<EncodedMediaFile> encodedFiles =  fileEncodingService.encodeAll(savedPaths);
            jobRepository.saveEncodedMediaFiles(job.getId(), encodedFiles);

            jobRepository.updateFileUploadStatusWhereId(job.getId(), true); 


            advance(job.getId());

        } catch (Exception e) {
            log.error("Upload processing failed for job {}", job.getId(), e);

            fail(job, e.getMessage());

            throw new UploadProcessingException(
                    "Upload processing failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Path> saveMovieFiles(
            FileUploadMetadata.MovieUploadMetadataDto metadata,
            Map<String, MultipartFile> fileMap, Path movieMediaDir, UploadJob job)
            throws IOException {

        List<FileUploadMetadata.UploadFileEntryDto> files = metadata.files();

        Map<String, Path> savedPaths = new HashMap<>();

        int total = files.size();
        int done = 0;

        /*
         * Assets:
         *
         * temp/assets/movies/{movieTmdbId}/
         */
        Path assetDir = tempMediaRoot.resolve("assets").resolve("movies").resolve(  String.valueOf(metadata.tmdbId()));

        Files.createDirectories(assetDir);

        tmdbImageService.download( metadata.posterPath(), assetDir.resolve("poster.jpg"), tempMediaRoot);

        tmdbImageService.download(metadata.backdropPath(), assetDir.resolve("backdrop.jpg"), tempMediaRoot);

        /*
         * Media:
         *
         * temp/movies/{movieTmdbId}/{filename}
         */
        Files.createDirectories(movieMediaDir);

        for (FileUploadMetadata.UploadFileEntryDto entry : files) {
            MultipartFile multipartFile = fileMap.get(entry.key());

            if (multipartFile == null) {
                throw new IllegalArgumentException("Missing uploaded file for key: " + entry.key());
            }

            String filename = Paths.get(Objects.requireNonNull(multipartFile.getOriginalFilename())).getFileName().toString();

            Path destination = movieMediaDir.resolve(filename);
            Files.deleteIfExists(destination);
            multipartFile.transferTo(destination);

            savedPaths.put(entry.key(), destination);

            done++;

            // Reserve the last 20% of progress for artwork.
        }

        return savedPaths;
    }

    private Map<String, Path> saveShowFiles(FileUploadMetadata.ShowUploadMetadataDto metadata,Map<String, MultipartFile> fileMap, Path showMediaDir, UploadJob job) throws IOException
    {
        Map<String, Path> savedPaths = new HashMap<>();

        /*
         * Show assets:
         *
         * temp/assets/shows/{showTmdbId}/
         */
        Path showAssetDir =
                tempMediaRoot.resolve("assets").resolve("shows").resolve(String.valueOf(metadata.tmdbId()));

        Files.createDirectories(showAssetDir);

        tmdbImageService.download(metadata.posterPath(),showAssetDir.resolve("poster.jpg"), tempMediaRoot);

        tmdbImageService.download(metadata.backdropPath(),showAssetDir.resolve("backdrop.jpg"), tempMediaRoot);

        /*
         * Show media:
         *
         * temp/shows/{showTmdbId}/
         */
        Files.createDirectories(showMediaDir);

        for (FileUploadMetadata.SeasonUploadMetadataDto season : metadata.seasons()) {
            /*
             * Assets:
             *
             * temp/assets/shows/{showTmdbId}/
             *     seasons/{seasonTmdbId}/
             */
            Path seasonAssetDir = showAssetDir.resolve("seasons").resolve( String.valueOf(season.tmdbId()));

            Files.createDirectories(seasonAssetDir);

            tmdbImageService.download(season.posterPath(), seasonAssetDir.resolve("poster.jpg"), tempMediaRoot);

            for (FileUploadMetadata.EpisodeUploadMetadataDto episode : season.episodes()) {
                /*
                 * Assets:
                 *
                 * temp/assets/shows/{showTmdbId}/
                 *     seasons/{seasonTmdbId}/
                 *         episodes/{episodeTmdbId}/
                 */
                Path episodeAssetDir = seasonAssetDir.resolve("episodes") .resolve(String.valueOf(episode.tmdbId()));

                Files.createDirectories(episodeAssetDir);

                tmdbImageService.download(episode.stillPath(), episodeAssetDir.resolve("still.jpg"), tempMediaRoot);

                /*
                 * Media:
                 *
                 * temp/shows/{showTmdbId}/
                 *     seasons/{seasonTmdbId}/
                 *         episodes/{episodeTmdbId}/
                 */
                Path episodeMediaDir = showMediaDir.resolve("seasons").resolve(String.valueOf(season.tmdbId())).resolve("episodes").resolve(String.valueOf(episode.tmdbId()));

                Files.createDirectories(episodeMediaDir);

                for (FileUploadMetadata.UploadFileEntryDto entry : episode.files()) {
                    MultipartFile multipartFile = fileMap.get(entry.key());

                    if (multipartFile == null) {
                        throw new IllegalArgumentException( "Missing uploaded file for key: " + entry.key());
                    }

                    String filename = Paths .get(Objects.requireNonNull( multipartFile.getOriginalFilename())).getFileName().toString();

                    Path destination = episodeMediaDir.resolve(filename);
                    Files.deleteIfExists(destination);
                    multipartFile.transferTo(destination);

                    savedPaths.put(entry.key(), destination);
                }
            }
        }

        return savedPaths;
    }



    @Transactional
    public void appendErrorMessageById(UUID uuid, String message) {

        jobRepository.appendErrorMessage(uuid, message);
    }
    @Transactional
    public void updateMediaEnrichmentStatusById(UUID uuid, boolean b) {

        jobRepository.updateMediaEnrichmentStatusWhereId(uuid, b);
    }
}