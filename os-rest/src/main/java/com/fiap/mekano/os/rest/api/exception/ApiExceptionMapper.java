package com.fiap.mekano.os.rest.api.exception;

import com.fiap.mekano.os.domain.exception.AppException;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Converte exceções em {@code application/problem+json}. */
@Provider
@ApplicationScoped
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof AppException app) {
            return build(app.getStatus(), app.getMessage());
        }
        if (exception instanceof WebApplicationException web) {
            return build(web.getResponse().getStatus(), web.getMessage());
        }
        Log.errorf(exception, "Unhandled exception: %s", exception.getMessage());
        return build(500, "Erro interno do servidor");
    }

    private Response build(int status, String detail) {
        String title = Response.Status.fromStatusCode(status) != null
                ? Response.Status.fromStatusCode(status).getReasonPhrase()
                : "Error";
        String instance = uriInfo != null ? uriInfo.getPath() : null;
        ProblemDetail body = new ProblemDetail("about:blank", title, status, detail, instance);
        return Response.status(status).type(PROBLEM_JSON).entity(body).build();
    }
}
