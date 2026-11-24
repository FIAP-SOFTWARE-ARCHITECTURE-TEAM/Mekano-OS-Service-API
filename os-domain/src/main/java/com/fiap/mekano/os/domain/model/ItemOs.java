package com.fiap.mekano.os.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fiap.mekano.os.domain.exception.AppException;
import com.fiap.mekano.os.domain.exception.Messages;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Item (peça ou serviço) de uma Ordem de Serviço.
 *
 * <p>Guarda um <b>snapshot</b> da descrição e do valor unitário no momento da criação:
 * mudanças posteriores no catálogo do monolito não afetam a OS. {@code referenciaUuid}
 * aponta para a peça ou serviço de origem (sem FK cross-contexto).
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class ItemOs {

    private final UUID id;
    private final UUID referenciaUuid;
    private final TipoItemOs tipo;
    private final String descricao;
    private final BigDecimal valorUnitario;
    private final Long quantidade;
    private final LocalDateTime createdAt;

    /** Cria um item novo, validando e normalizando os dados. */
    public static ItemOs create(UUID referenciaUuid, TipoItemOs tipo, String descricao,
                                BigDecimal valorUnitario, Long quantidade) {
        validar(referenciaUuid, tipo, descricao, valorUnitario, quantidade);
        return ItemOs.builder()
                .id(UUID.randomUUID())
                .referenciaUuid(referenciaUuid)
                .tipo(tipo)
                .descricao(descricao.strip())
                .valorUnitario(valorUnitario)
                .quantidade(quantidade)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /** Reconstrói um item a partir de dados persistidos (não gera UUID nem timestamp). */
    public static ItemOs reconstitute(UUID id, UUID referenciaUuid, TipoItemOs tipo, String descricao,
                                      BigDecimal valorUnitario, Long quantidade, LocalDateTime createdAt) {
        validar(referenciaUuid, tipo, descricao, valorUnitario, quantidade);
        return ItemOs.builder()
                .id(id)
                .referenciaUuid(referenciaUuid)
                .tipo(tipo)
                .descricao(descricao)
                .valorUnitario(valorUnitario)
                .quantidade(quantidade)
                .createdAt(createdAt)
                .build();
    }

    public boolean isPeca() {
        return tipo == TipoItemOs.PECA;
    }

    public boolean isServico() {
        return tipo == TipoItemOs.SERVICO;
    }

    /** Valor total do item (valor unitário × quantidade). */
    public BigDecimal getValorTotal() {
        return valorUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    private static void validar(UUID referenciaUuid, TipoItemOs tipo, String descricao,
                                BigDecimal valorUnitario, Long quantidade) {
        if (referenciaUuid == null) {
            throw AppException.badRequest(Messages.get("itemos.referencia.required"));
        }
        if (tipo == null) {
            throw AppException.badRequest(Messages.get("itemos.tipo.required"));
        }
        if (descricao == null || descricao.isBlank()) {
            throw AppException.badRequest(Messages.get("itemos.descricao.required"));
        }
        if (valorUnitario == null || valorUnitario.signum() < 0) {
            throw AppException.badRequest(Messages.get("itemos.valor.invalido"));
        }
        if (quantidade == null || quantidade <= 0) {
            throw AppException.badRequest(Messages.get("itemos.quantidade.invalida"));
        }
    }
}
