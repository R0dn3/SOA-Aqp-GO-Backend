package com.integrador.Turismo.Soap;

import com.integrador.Turismo.Service.OrquestadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
@RequiredArgsConstructor
public class OrquestadorEndpoint {

    private static final String NAMESPACE_URI = "http://aqpgo.com/orquestador";

    private final OrquestadorService orquestadorService;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "reservarYPagarRequest")
    @ResponsePayload
    public ReservarYPagarResponse reservarYPagar(@RequestPayload ReservarYPagarRequest request) {
        return orquestadorService.reservarYPagar(request);
    }
}