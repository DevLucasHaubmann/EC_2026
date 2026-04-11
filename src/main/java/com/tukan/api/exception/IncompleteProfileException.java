package com.tukan.api.exception;

import lombok.Getter;

@Getter
public class IncompleteProfileException extends RuntimeException {

    private final String campoAusente;

    public IncompleteProfileException(String campoAusente, String message) {
        super(message);
        this.campoAusente = campoAusente;
    }
}
