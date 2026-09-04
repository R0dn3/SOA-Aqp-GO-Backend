package com.integrador.Turismo.Soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "crearReservaRequest", namespace = "http://aqpgo.com/reservas")
@XmlAccessorType(XmlAccessType.FIELD)
public class CrearReservaRequest {

    @XmlElement(name = "usuarioId", namespace = "http://aqpgo.com/reservas")
    private String usuarioId;

    @XmlElement(name = "paqueteId", namespace = "http://aqpgo.com/reservas")
    private String paqueteId;

    @XmlElement(name = "fechaSalida", namespace = "http://aqpgo.com/reservas")
    private String fechaSalida;

    @XmlElement(name = "numPersonas", namespace = "http://aqpgo.com/reservas")
    private int numPersonas;

    @XmlElement(name = "acompanante", namespace = "http://aqpgo.com/reservas")
    private List<AcompananteItem> acompanante = new ArrayList<>();

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getPaqueteId() {
        return paqueteId;
    }

    public void setPaqueteId(String paqueteId) {
        this.paqueteId = paqueteId;
    }

    public String getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(String fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public int getNumPersonas() {
        return numPersonas;
    }

    public void setNumPersonas(int numPersonas) {
        this.numPersonas = numPersonas;
    }

    public List<AcompananteItem> getAcompanante() {
        return acompanante;
    }

    public void setAcompanante(List<AcompananteItem> acompanante) {
        this.acompanante = acompanante;
    }
}