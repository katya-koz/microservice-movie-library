package com.katyaflix.uploadservice.service;

public class TmdbRequestException extends RuntimeException {

    private final int status;

    public TmdbRequestException(String message, int status) {
        super(message);
        this.status = status;
    }

    public TmdbRequestException(String message, int status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}