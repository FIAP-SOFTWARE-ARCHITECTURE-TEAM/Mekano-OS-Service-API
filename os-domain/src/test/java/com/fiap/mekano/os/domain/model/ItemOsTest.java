package com.fiap.mekano.os.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.mekano.os.domain.exception.AppException;

class ItemOsTest {

    private static final UUID REF = UUID.randomUUID();

    @Test
    void createGuardaSnapshotENormaliza() {
        ItemOs item = ItemOs.create(REF, TipoItemOs.PECA, "  Filtro de óleo ", new BigDecimal("25.50"), 2L);

        assertThat(item.getId()).isNotNull();
        assertThat(item.getDescricao()).isEqualTo("Filtro de óleo");
        assertThat(item.getValorTotal()).isEqualByComparingTo("51.00");
        assertThat(item.isPeca()).isTrue();
        assertThat(item.isServico()).isFalse();
        assertThat(item.getCreatedAt()).isNotNull();
    }

    @Test
    void reconstitutePreservaIdETimestamp() {
        UUID id = UUID.randomUUID();
        LocalDateTime criado = LocalDateTime.of(2026, 1, 1, 10, 0);

        ItemOs item = ItemOs.reconstitute(id, REF, TipoItemOs.SERVICO, "Alinhamento", BigDecimal.TEN, 1L, criado);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getCreatedAt()).isEqualTo(criado);
        assertThat(item.isServico()).isTrue();
    }

    @Test
    void valorZeroEPermitidoMasNegativoNao() {
        assertThat(ItemOs.create(REF, TipoItemOs.SERVICO, "Cortesia", BigDecimal.ZERO, 1L).getValorTotal())
                .isEqualByComparingTo("0");
        assertThatThrownBy(() -> ItemOs.create(REF, TipoItemOs.SERVICO, "x", new BigDecimal("-1"), 1L))
                .isInstanceOf(AppException.class);
    }

    @Test
    void rejeitaDadosInvalidos() {
        assertInvalido(() -> ItemOs.create(null, TipoItemOs.PECA, "x", BigDecimal.ONE, 1L));
        assertInvalido(() -> ItemOs.create(REF, null, "x", BigDecimal.ONE, 1L));
        assertInvalido(() -> ItemOs.create(REF, TipoItemOs.PECA, " ", BigDecimal.ONE, 1L));
        assertInvalido(() -> ItemOs.create(REF, TipoItemOs.PECA, null, BigDecimal.ONE, 1L));
        assertInvalido(() -> ItemOs.create(REF, TipoItemOs.PECA, "x", null, 1L));
        assertInvalido(() -> ItemOs.create(REF, TipoItemOs.PECA, "x", BigDecimal.ONE, 0L));
        assertInvalido(() -> ItemOs.create(REF, TipoItemOs.PECA, "x", BigDecimal.ONE, null));
    }

    private static void assertInvalido(Runnable r) {
        assertThatThrownBy(r::run)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(400);
    }
}
