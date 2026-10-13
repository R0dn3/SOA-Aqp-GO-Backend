package com.integrador.Turismo.Mensajeria;

/**
 * Contrato de un suscriptor del ESB (coreografía): cada suscriptor decide por
 * sí mismo qué eventos le interesan y cómo reaccionar. El orquestador no sabe
 * que existen.
 */
public interface SuscriptorEsb {

    /** Nombre con el que aparece en logs, métricas y dead letter. */
    String nombre();

    /** Indica si este suscriptor reacciona a ese tipo de evento. */
    boolean escucha(EventoReserva.Tipo tipo);

    /** Reacciona al evento. Si lanza una excepción, la cola reintenta. */
    void procesar(EventoReserva evento);
}