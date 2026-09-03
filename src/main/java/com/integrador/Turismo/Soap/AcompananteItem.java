package com.integrador.Turismo.Soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "acompananteItem", namespace = "http://aqpgo.com/reservas")
@XmlAccessorType(XmlAccessType.FIELD)
public class AcompananteItem {

    @XmlElement(name = "nombreCompleto", namespace = "http://aqpgo.com/reservas")
    private String nombreCompleto;

    @XmlElement(name = "dniPasaporte", namespace = "http://aqpgo.com/reservas")
    private String dniPasaporte;

    @XmlElement(name = "pais", namespace = "http://aqpgo.com/reservas")
    private String pais;

    @XmlElement(name = "fechaNacimiento", namespace = "http://aqpgo.com/reservas")
    private String fechaNacimiento;

    @XmlElement(name = "genero", namespace = "http://aqpgo.com/reservas")
    private String genero;

    @XmlElement(name = "datosAdicionales", namespace = "http://aqpgo.com/reservas")
    private String datosAdicionales;

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getDniPasaporte() { return dniPasaporte; }
    public void setDniPasaporte(String dniPasaporte) { this.dniPasaporte = dniPasaporte; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getDatosAdicionales() { return datosAdicionales; }
    public void setDatosAdicionales(String datosAdicionales) { this.datosAdicionales = datosAdicionales; }
}