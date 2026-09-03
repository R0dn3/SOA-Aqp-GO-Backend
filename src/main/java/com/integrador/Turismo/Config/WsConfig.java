package com.integrador.Turismo.Config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class WsConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            WebApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
                com.integrador.Turismo.Soap.ConsultarReservaRequest.class,
                com.integrador.Turismo.Soap.ConsultarReservaResponse.class,
                com.integrador.Turismo.Soap.CrearReservaRequest.class,
                com.integrador.Turismo.Soap.CrearReservaResponse.class,
                com.integrador.Turismo.Soap.ListarReservasPorUsuarioRequest.class,
                com.integrador.Turismo.Soap.ListarReservasPorUsuarioResponse.class,
                com.integrador.Turismo.Soap.ReservaItem.class,
                com.integrador.Turismo.Soap.AcompananteItem.class);
        return marshaller;
    }

    @Bean(name = "reservas")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema reservasSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("ReservasPort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace("http://aqpgo.com/reservas");
        wsdl11Definition.setSchema(reservasSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema reservasSchema() {
        return new SimpleXsdSchema(new ClassPathResource("reserva.xsd"));
    }
}
