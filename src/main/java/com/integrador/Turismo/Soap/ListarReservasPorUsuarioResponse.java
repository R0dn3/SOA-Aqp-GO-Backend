package com.integrador.Turismo.Soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "listarReservasPorUsuarioResponse", namespace = "http://aqpgo.com/reservas")
@XmlAccessorType(XmlAccessType.FIELD)
public class ListarReservasPorUsuarioResponse {

    @XmlElement(name = "reserva", namespace = "http://aqpgo.com/reservas")
    private List<ReservaItem> reserva = new ArrayList<>();

    public List<ReservaItem> getReserva() {
        return reserva;
    }

    public void setReserva(List<ReservaItem> reserva) {
        this.reserva = reserva;
    }
}