package com.integrador.Turismo.Soap;

import com.integrador.Turismo.DTO.ReservaResponse;
import com.integrador.Turismo.Service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import com.integrador.Turismo.DTO.ReservaRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Endpoint
@RequiredArgsConstructor
public class ReservaEndpoint {

    private static final String NAMESPACE_URI = "http://aqpgo.com/reservas";

    private final ReservaService reservaService;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "consultarReservaRequest")
    @ResponsePayload
    public ConsultarReservaResponse consultarReserva(@RequestPayload ConsultarReservaRequest request) {
        ReservaResponse reserva = reservaService.obtenerPorId(request.getId());

        ConsultarReservaResponse response = new ConsultarReservaResponse();
        response.setId(reserva.id());
        response.setPaqueteId(reserva.paqueteId());
        response.setPaqueteNombre(reserva.paqueteNombre());
        response.setFotoPrincipal(reserva.fotoPrincipal());
        response.setFechaSalida(reserva.fechaSalida().toString());
        response.setNumPersonas(reserva.numPersonas());
        response.setPrecioTotal(reserva.precioTotal().toString());
        response.setEstado(reserva.estado());
        response.setCreatedAt(reserva.createdAt().toString());

        List<AcompananteItem> acompanantes = new ArrayList<>();
        for (var a : reserva.acompanantes()) {
            AcompananteItem ai = new AcompananteItem();
            ai.setNombreCompleto(a.nombreCompleto());
            ai.setDniPasaporte(a.dniPasaporte());
            ai.setPais(a.pais());
            ai.setFechaNacimiento(a.fechaNacimiento() != null ? a.fechaNacimiento().toString() : null);
            ai.setGenero(a.genero());
            ai.setDatosAdicionales(a.datosAdicionales());
            acompanantes.add(ai);
        }
        response.setAcompanante(acompanantes);

        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "crearReservaRequest")
    @ResponsePayload
    public CrearReservaResponse crearReserva(@RequestPayload CrearReservaRequest request) {
        ReservaRequest req = new ReservaRequest(
                request.getPaqueteId(),
                LocalDate.parse(request.getFechaSalida()),
                request.getNumPersonas(),
                java.util.Collections.emptyList());

        ReservaResponse reserva = reservaService.crear(req, request.getUsuarioId());

        CrearReservaResponse response = new CrearReservaResponse();
        response.setId(reserva.id());
        response.setPaqueteNombre(reserva.paqueteNombre());
        response.setFechaSalida(reserva.fechaSalida().toString());
        response.setNumPersonas(reserva.numPersonas());
        response.setPrecioTotal(reserva.precioTotal().toString());
        response.setEstado(reserva.estado());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "listarReservasPorUsuarioRequest")
    @ResponsePayload
    public ListarReservasPorUsuarioResponse listarReservasPorUsuario(
            @RequestPayload ListarReservasPorUsuarioRequest request) {
        List<ReservaResponse> reservas = reservaService.misReservas(request.getUsuarioId());

        List<ReservaItem> items = new ArrayList<>();
        for (ReservaResponse r : reservas) {
            ReservaItem item = new ReservaItem();
            item.setId(r.id());
            item.setPaqueteId(r.paqueteId());
            item.setPaqueteNombre(r.paqueteNombre());
            item.setFotoPrincipal(r.fotoPrincipal());
            item.setFechaSalida(r.fechaSalida().toString());
            item.setNumPersonas(r.numPersonas());
            item.setPrecioTotal(r.precioTotal().toString());
            item.setEstado(r.estado());
            item.setCreatedAt(r.createdAt().toString());

            List<AcompananteItem> acompanantes = new ArrayList<>();
            for (var a : r.acompanantes()) {
                AcompananteItem ai = new AcompananteItem();
                ai.setNombreCompleto(a.nombreCompleto());
                ai.setDniPasaporte(a.dniPasaporte());
                ai.setPais(a.pais());
                ai.setFechaNacimiento(a.fechaNacimiento() != null ? a.fechaNacimiento().toString() : null);
                ai.setGenero(a.genero());
                ai.setDatosAdicionales(a.datosAdicionales());
                acompanantes.add(ai);
            }
            item.setAcompanante(acompanantes);

            items.add(item);
        }

        ListarReservasPorUsuarioResponse response = new ListarReservasPorUsuarioResponse();
        response.setReserva(items);
        return response;
    }
}