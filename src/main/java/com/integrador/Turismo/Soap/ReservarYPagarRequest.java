//ReservarYPagarRequest.java
package com.integrador.Turismo.Soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "reservarYPagarRequest", namespace = "http://aqpgo.com/orquestador")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReservarYPagarRequest {

    @XmlElement(name = "usuarioId", namespace = "http://aqpgo.com/orquestador")
    private String usuarioId;

    @XmlElement(name = "paqueteId", namespace = "http://aqpgo.com/orquestador")
    private String paqueteId;

    @XmlElement(name = "fechaSalida", namespace = "http://aqpgo.com/orquestador")
    private String fechaSalida;

    @XmlElement(name = "numPersonas", namespace = "http://aqpgo.com/orquestador")
    private int numPersonas;

    @XmlElement(name = "acompanante", namespace = "http://aqpgo.com/orquestador")
    private List<AcompananteOrqItem> acompanante = new ArrayList<>();

    @XmlElement(name = "monto", namespace = "http://aqpgo.com/orquestador")
    private String monto;

    @XmlElement(name = "metodo", namespace = "http://aqpgo.com/orquestador")
    private String metodo;

    @XmlElement(name = "referencia", namespace = "http://aqpgo.com/orquestador")
    private String referencia;

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

    public List<AcompananteOrqItem> getAcompanante() {
        return acompanante;
    }

    public String getMonto() {
        return monto;
    }

    public void setMonto(String monto) {
        this.monto = monto;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}