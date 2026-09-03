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
    private final PagoRepository    pagoRepository;
    private final ReservaRepository reservaRepository;

    @Transactional
    public PagoResponse procesarPago(PagoRequest req) {
        Reserva reserva = reservaRepository.findById(req.reservaId())
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (reserva.getEstado() == Reserva.Estado.CONFIRMADA) {
            throw new IllegalStateException("Esta reserva ya está pagada");
        }

        // Pasarela simulada: siempre aprueba si el monto coincide
        boolean aprobado = req.monto().compareTo(reserva.getPrecioTotal()) == 0;

        Pago pago = Pago.builder()
                .reserva(reserva)
                .monto(req.monto())
                .metodo(req.metodo())
                .estado(aprobado ? Pago.Estado.VERIFICADO : Pago.Estado.RECHAZADO)
                .referencia(req.referencia())
                .fechaPago(LocalDateTime.now())
                .build();

        pagoRepository.save(pago);

        // Si el pago fue aprobado, confirma la reserva
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
                pago.getFechaPago()
        );
    }
}
