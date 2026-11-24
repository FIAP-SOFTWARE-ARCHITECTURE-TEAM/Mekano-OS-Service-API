package com.fiap.mekano.os.infrastructure.cache;

/** Nomes dos caches Caffeine configurados em {@code cache-config.yml}. */
public final class CacheNames {

    public static final String CLIENTES = "clientes";
    public static final String VEICULOS = "veiculos";
    public static final String SERVICOS = "servicos";
    public static final String PECAS = "pecas";
    public static final String ORDENS_SERVICO = "ordens_servico";

    private CacheNames() {
    }
}
