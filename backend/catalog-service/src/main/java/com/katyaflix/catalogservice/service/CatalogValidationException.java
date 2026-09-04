package com.katyaflix.catalogservice.service;

public class CatalogValidationException extends RuntimeException {
    public CatalogValidationException(String message) {
        super(message);
    }
}
