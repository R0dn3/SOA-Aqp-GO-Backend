// src/main/java/com/integrador/Turismo/Security/JwtService.java
package com.integrador.Turismo.Soap;

import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Service.OrquestadorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@RequiredArgsConstructor
@Slf4j
public class OrquestadorEndpoint {

    private static final String NAMESPACE_URI = "http://aqpgo.com/orquestador";

    private final OrquestadorService orquestadorService;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "reservarYPagarRequest")
    @ResponsePayload
    public ReservarYPagarResponse reservarYPagar(@RequestPayload ReservarYPagarRequest request) {

        // [ESB-SECURITY] La identidad sale del JWT, nunca del mensaje.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario usuario)) {
            log.warn("[ESB-SECURITY] Petición rechazada: no hay JWT válido");
            throw new AccessDeniedException("Se requiere autenticación (JWT) para invocar reservarYPagar");
        }

        if (request.getUsuarioId() != null && !request.getUsuarioId().equals(usuario.getId())) {
            log.warn("[ESB-SECURITY] usuarioId del mensaje ({}) no coincide con el del token ({}); se usa el del token",
                    request.getUsuarioId(), usuario.getId());
        }
        request.setUsuarioId(usuario.getId());
        log.info("[ESB-SECURITY] Identidad tomada del JWT: usuarioId={}", usuario.getId());

        return orquestadorService.reservarYPagar(request);
    }
}