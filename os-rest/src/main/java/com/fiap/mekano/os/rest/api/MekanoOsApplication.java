package com.fiap.mekano.os.rest.api;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

import jakarta.ws.rs.core.Application;

@OpenAPIDefinition(info = @Info(title = "Mekano OS Service API", version = "1.0.0",
        description = "Microserviço de Ordem de Serviço — FIAP Software Architecture"))
public class MekanoOsApplication extends Application {
}
