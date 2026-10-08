package com.integrador.Turismo.Soap;

import com.integrador.Turismo.Model.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** [ESB-SECURITY] La identidad sale del JWT, nunca del mensaje. */
@Slf4j
public final class SoapAuth {

    private SoapAuth() {
    }

    public static Usuario usuarioAutenticado(String operacion) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario usuario)) {
            log.warn("[ESB-SECURITY] Petición rechazada en {}: no hay JWT válido", operacion);
            throw new AccessDeniedException("Se requiere autenticación (JWT) para invocar " + operacion);
        }
        return usuario;
    }

    /** Devuelve el id del token; si el mensaje traía otro, lo avisa y lo ignora. */
    public static String usuarioId(Usuario usuario, String idDelMensaje, String operacion) {
        if (idDelMensaje != null && !idDelMensaje.equals(usuario.getId())) {
            log.warn(
                    "[ESB-SECURITY] {}: usuarioId del mensaje ({}) no coincide con el del token ({}); se usa el del token",
                    operacion, idDelMensaje, usuario.getId());
        }
        log.info("[ESB-SECURITY] {}: identidad tomada del JWT usuarioId={}", operacion, usuario.getId());
        return usuario.getId();
    }
}