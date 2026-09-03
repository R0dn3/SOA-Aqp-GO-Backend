package com.integrador.Turismo.Soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "listarReservasPorUsuarioRequest", namespace = "http://aqpgo.com/reservas")
@XmlAccessorType(XmlAccessType.FIELD)
public class ListarReservasPorUsuarioRequest {

    @XmlElement(name = "usuarioId", namespace = "http://aqpgo.com/reservas")
    private String usuarioId;

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }
}