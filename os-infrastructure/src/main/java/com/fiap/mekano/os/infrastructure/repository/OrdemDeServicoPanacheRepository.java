package com.fiap.mekano.os.infrastructure.repository;

import com.fiap.mekano.os.infrastructure.entity.OrdemDeServicoEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrdemDeServicoPanacheRepository implements PanacheRepository<OrdemDeServicoEntity> {
}
