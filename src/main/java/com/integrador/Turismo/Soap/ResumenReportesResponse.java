package com.integrador.Turismo.Soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "resumenReportesResponse", namespace = "http://aqpgo.com/reportes")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResumenReportesResponse {

    @XmlElement(name = "ingresosEsteMes", namespace = "http://aqpgo.com/reportes")
    private String ingresosEsteMes;

    @XmlElement(name = "totalReservas", namespace = "http://aqpgo.com/reportes")
    private int totalReservas;

    @XmlElement(name = "confirmadas", namespace = "http://aqpgo.com/reportes")
    private int confirmadas;

    @XmlElement(name = "crecimientoIngresos", namespace = "http://aqpgo.com/reportes")
    private double crecimientoIngresos;

    @XmlElement(name = "nuevosClientesMes", namespace = "http://aqpgo.com/reportes")
    private int nuevosClientesMes;

    public String getIngresosEsteMes() {
        return ingresosEsteMes;
    }

    public void setIngresosEsteMes(String ingresosEsteMes) {
        this.ingresosEsteMes = ingresosEsteMes;
    }

    public int getTotalReservas() {
        return totalReservas;
    }

    public void setTotalReservas(int totalReservas) {
        this.totalReservas = totalReservas;
    }

    public int getConfirmadas() {
        return confirmadas;
    }

    public void setConfirmadas(int confirmadas) {
        this.confirmadas = confirmadas;
    }

    public double getCrecimientoIngresos() {
        return crecimientoIngresos;
    }

    public void setCrecimientoIngresos(double crecimientoIngresos) {
        this.crecimientoIngresos = crecimientoIngresos;
    }

    public int getNuevosClientesMes() {
        return nuevosClientesMes;
    }

    public void setNuevosClientesMes(int nuevosClientesMes) {
        this.nuevosClientesMes = nuevosClientesMes;
    }
}