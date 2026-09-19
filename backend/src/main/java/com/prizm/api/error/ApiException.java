package com.prizm.api.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public ApiException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public static ApiException badRequest(String error, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, error, message);
    }

    public static ApiException notFound(String error, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, error, message);
    }

    public static ApiException forbidden(String error, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, error, message);
    }
}
