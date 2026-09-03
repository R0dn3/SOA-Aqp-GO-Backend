package com.integrador.Turismo.Soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "consultarReservaRequest", namespace = "http://aqpgo.com/reservas")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConsultarReservaRequest {

    @XmlElement(name = "id", namespace = "http://aqpgo.com/reservas")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
