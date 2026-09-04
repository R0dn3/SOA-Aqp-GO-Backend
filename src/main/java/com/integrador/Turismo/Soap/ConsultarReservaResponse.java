package com.integrador.Turismo.Soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "consultarReservaResponse", namespace = "http://aqpgo.com/reservas")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConsultarReservaResponse {

    @XmlElement(name = "id", namespace = "http://aqpgo.com/reservas")
    private String id;

    @XmlElement(name = "paqueteId", namespace = "http://aqpgo.com/reservas")
    private String paqueteId;

    @XmlElement(name = "paqueteNombre", namespace = "http://aqpgo.com/reservas")
    private String paqueteNombre;

    @XmlElement(name = "fotoPrincipal", namespace = "http://aqpgo.com/reservas")
    private String fotoPrincipal;

    @XmlElement(name = "fechaSalida", namespace = "http://aqpgo.com/reservas")
    private String fechaSalida;

    @XmlElement(name = "numPersonas", namespace = "http://aqpgo.com/reservas")
    private int numPersonas;

    @XmlElement(name = "precioTotal", namespace = "http://aqpgo.com/reservas")
    private String precioTotal;

    @XmlElement(name = "estado", namespace = "http://aqpgo.com/reservas")
    private String estado;

    @XmlElement(name = "createdAt", namespace = "http://aqpgo.com/reservas")
    private String createdAt;

    @XmlElement(name = "acompanante", namespace = "http://aqpgo.com/reservas")
    private List<AcompananteItem> acompanante = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPaqueteId() {
        return paqueteId;
    }

    public void setPaqueteId(String paqueteId) {
        this.paqueteId = paqueteId;
    }

    public String getPaqueteNombre() {
        return paqueteNombre;
    }

    public void setPaqueteNombre(String paqueteNombre) {
        this.paqueteNombre = paqueteNombre;
    }

    public String getFotoPrincipal() {
        return fotoPrincipal;
    }

    public void setFotoPrincipal(String fotoPrincipal) {
        this.fotoPrincipal = fotoPrincipal;
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

    public String getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(String precioTotal) {
        this.precioTotal = precioTotal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<AcompananteItem> getAcompanante() {
        return acompanante;
    }

    public void setAcompanante(List<AcompananteItem> acompanante) {
        this.acompanante = acompanante;
    }
}