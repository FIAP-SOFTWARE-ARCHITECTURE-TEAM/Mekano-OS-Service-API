package com.fiap.mekano.os.infrastructure.audit;

import java.io.Serializable;

import io.quarkus.arc.Unremovable;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/** Expõe o principal (subject do JWT) do usuário autenticado na requisição corrente. */
@RequestScoped
@Unremovable
public class AuditoriaContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Instance<SecurityIdentity> identity;

    @Inject
    public AuditoriaContext(Instance<SecurityIdentity> identity) {
        this.identity = identity;
    }

    public String principalName() {
        try {
            SecurityIdentity securityIdentity = identity.get();
            if (securityIdentity == null || securityIdentity.isAnonymous()) {
                return null;
            }
            return securityIdentity.getPrincipal().getName();
        } catch (Exception e) {
            return null;
        }
    }
}
