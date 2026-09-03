package com.integrador.Turismo.Soap;

import com.integrador.Turismo.DTO.ResumenReportesDto;
import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Repository.UsuarioRepository;
import com.integrador.Turismo.Security.JwtService;
import com.integrador.Turismo.Service.ReporteService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@RequiredArgsConstructor
public class ReporteEndpoint {

    private static final String NAMESPACE_URI = "http://aqpgo.com/reportes";

    private final ReporteService reporteService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "resumenReportesRequest")
    @ResponsePayload
    public ResumenReportesResponse resumenReportes(@RequestPayload ResumenReportesRequest request) {
        String email;
        try {
            email = jwtService.extractEmail(request.getToken());
        } catch (JwtException e) {
            throw new RuntimeException("Token inválido o expirado");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getRol() != Usuario.Rol.ADMIN) {
            throw new RuntimeException("No autorizado: se requiere rol ADMIN");
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