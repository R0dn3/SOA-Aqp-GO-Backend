package com.integrador.Turismo.Soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "resumenReportesRequest", namespace = "http://aqpgo.com/reportes")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResumenReportesRequest {

    @XmlElement(name = "token", namespace = "http://aqpgo.com/reportes")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}