package com.fiap.mekano.os.infrastructure.client;

import java.util.List;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

/** Propaga o header {@code Authorization} da requisição atual para o monolito Mekano-API. */
@ApplicationScoped
public class AuthorizationHeaderFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        if (incomingHeaders != null) {
            List<String> auth = incomingHeaders.get(HttpHeaders.AUTHORIZATION);
            if (auth != null && !auth.isEmpty()) {
                headers.put(HttpHeaders.AUTHORIZATION, auth);
            }
        }
        return headers;
    }
}
