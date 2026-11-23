package com.fiap.mekano.os.domain.exception;

import lombok.Getter;

/**
 * Exceção única de negócio/validação do serviço de OS.
 * Carrega o status HTTP semântico (mesmo padrão do Mekano-API).
 */
@Getter
public class AppException extends RuntimeException {

    private final int status;

    public AppException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static AppException badRequest(String message) {
        return new AppException(400, message);
    }

    public static AppException notFound(String message) {
        return new AppException(404, message);
    }

    public static AppException conflict(String message) {
        return new AppException(409, message);
    }

    public static AppException unprocessable(String message) {
        return new AppException(422, message);
    }
}
