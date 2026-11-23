package com.fiap.mekano.os.rest.api.exception;

/** Problem Details (RFC 7807). */
public record ProblemDetail(String type, String title, int status, String detail, String instance) {
}
