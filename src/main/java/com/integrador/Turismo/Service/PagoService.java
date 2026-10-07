package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.PagoRequest;
import com.integrador.Turismo.DTO.PagoResponse;
import com.integrador.Turismo.Model.Pago;
import com.integrador.Turismo.Model.Reserva;
import com.integrador.Turismo.Repository.PagoRepository;
import com.integrador.Turismo.Repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PagoService {
    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final MetodoPagoConfigService metodoPagoConfigService;

    @Transactional
    public PagoResponse procesarPago(PagoRequest req) {
        Reserva reserva = reservaRepository.findById(req.reservaId())
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (reserva.getEstado() == Reserva.Estado.CONFIRMADA) {
            throw new IllegalStateException("Esta reserva ya está pagada");
        }

        boolean montoCoincide = req.monto().compareTo(reserva.getPrecioTotal()) == 0;
        boolean metodoHabilitado = metodoPagoConfigService.estaHabilitado(req.metodo());
        boolean aprobado = montoCoincide && metodoHabilitado;

        String motivoRechazo = null;
        if (!aprobado) {
            motivoRechazo = !metodoHabilitado
                    ? "El método de pago " + req.metodo() + " no está disponible actualmente"
                    : "El monto no coincide con el total de la reserva";
        }

        Pago pago = Pago.builder()
                .reserva(reserva)
                .monto(req.monto())
                .metodo(req.metodo())
                .estado(aprobado ? Pago.Estado.VERIFICADO : Pago.Estado.RECHAZADO)
                .referencia(req.referencia())
                .fechaPago(LocalDateTime.now())
                .build();

        pagoRepository.save(pago);

        if (aprobado) {
            reserva.setEstado(Reserva.Estado.CONFIRMADA);
            reservaRepository.save(reserva);
        }

        return new PagoResponse(
                pago.getId(),
                reserva.getId(),
                pago.getMonto(),
                pago.getMetodo().name(),
                pago.getEstado().name(),
                pago.getReferencia(),
                pago.getFechaPago(),
                motivoRechazo);
    }
}