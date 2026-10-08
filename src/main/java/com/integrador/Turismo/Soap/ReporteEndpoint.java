//ReporteEndpoint.java
package com.integrador.Turismo.Soap;

import com.integrador.Turismo.DTO.ResumenReportesDto;
import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Service.ReporteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@RequiredArgsConstructor
@Slf4j
public class ReporteEndpoint {

    private static final String NAMESPACE_URI = "http://aqpgo.com/reportes";

    private final ReporteService reporteService;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "resumenReportesRequest")
    @ResponsePayload
    public ResumenReportesResponse resumenReportes(@RequestPayload ResumenReportesRequest request) {
        Usuario usuario = SoapAuth.usuarioAutenticado("resumenReportes");

        // [ESB-SECURITY] Autorización por rol: solo ADMIN.
        if (usuario.getRol() != Usuario.Rol.ADMIN) {
            log.warn("[ESB-SECURITY] usuarioId={} sin rol ADMIN intentó pedir resumenReportes", usuario.getId());
            throw new AccessDeniedException("No autorizado: se requiere rol ADMIN");
        }

        ResumenReportesDto resumen = reporteService.obtenerReportesCompletos().resumen();

        ResumenReportesResponse response = new ResumenReportesResponse();
        response.setIngresosEsteMes(resumen.ingresosEsteMes().toString());
        response.setTotalReservas(resumen.totalReservas());
        response.setConfirmadas(resumen.confirmadas());
        response.setCrecimientoIngresos(resumen.crecimientoIngresos());
        response.setNuevosClientesMes(resumen.nuevosClientesMes());

        return response;
    }
}