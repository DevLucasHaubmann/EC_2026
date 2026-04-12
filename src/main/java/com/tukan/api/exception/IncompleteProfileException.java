package com.tukan.api.exception;

import lombok.Getter;

@Getter
public class IncompleteProfileException extends RuntimeException {

    private final String missingField;

    public IncompleteProfileException(String missingField, String message) {
        super(message);
        this.missingField = missingField;
    }
}