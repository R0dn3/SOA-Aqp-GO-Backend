package com.integrador.Turismo.Soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "reservarYPagarResponse", namespace = "http://aqpgo.com/orquestador")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReservarYPagarResponse {

    @XmlElement(name = "reservaId", namespace = "http://aqpgo.com/orquestador")
    private String reservaId;

    @XmlElement(name = "estadoReserva", namespace = "http://aqpgo.com/orquestador")
    private String estadoReserva;

    @XmlElement(name = "pagoId", namespace = "http://aqpgo.com/orquestador")
    private String pagoId;

    @XmlElement(name = "estadoPago", namespace = "http://aqpgo.com/orquestador")
    private String estadoPago;

    @XmlElement(name = "mensaje", namespace = "http://aqpgo.com/orquestador")
    private String mensaje;

    // Getters y setters
    public String getReservaId() {
        return reservaId;
    }

    public void setReservaId(String reservaId) {
        this.reservaId = reservaId;
    }

    public String getEstadoReserva() {
        return estadoReserva;
    }

    public void setEstadoReserva(String estadoReserva) {
        this.estadoReserva = estadoReserva;
    }

    public String getPagoId() {
        return pagoId;
    }

    public void setPagoId(String pagoId) {
        this.pagoId = pagoId;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}