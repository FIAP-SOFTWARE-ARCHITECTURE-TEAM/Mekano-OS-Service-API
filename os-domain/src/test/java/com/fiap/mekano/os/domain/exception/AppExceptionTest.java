package com.fiap.mekano.os.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppExceptionTest {

    @Test
    void factoriesCarryHttpStatusAndMessage() {
        assertThat(AppException.badRequest("x").getStatus()).isEqualTo(400);
        assertThat(AppException.notFound("x").getStatus()).isEqualTo(404);
        assertThat(AppException.conflict("x").getStatus()).isEqualTo(409);
        assertThat(AppException.unprocessable("boom").getStatus()).isEqualTo(422);
        assertThat(AppException.unprocessable("boom")).hasMessage("boom");
    }
}
