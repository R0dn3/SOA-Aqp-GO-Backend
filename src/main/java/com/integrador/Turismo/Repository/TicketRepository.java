package com.integrador.Turismo.Repository;

import com.integrador.Turismo.Model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, String> {

    // Cliente — sus propios tickets (sin cargar mensajes)
    List<Ticket> findByUsuarioIdOrderByUpdatedAtDesc(String usuarioId);

    // Admin — todos con paginación para no traer miles de registros
    Page<Ticket> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    // Admin — filtrar por estado
    Page<Ticket> findByEstadoOrderByUpdatedAtDesc(Ticket.Estado estado, Pageable pageable);

    // Admin — filtrar por tipo
    Page<Ticket> findByTipoOrderByUpdatedAtDesc(Ticket.Tipo tipo, Pageable pageable);

    // Detalle con mensajes en una sola query — evita N+1
    // Carga el ticket + sus mensajes + los autores de cada mensaje
    @Query("""
        SELECT DISTINCT t FROM Ticket t
        LEFT JOIN FETCH t.mensajes m
        LEFT JOIN FETCH m.autor
        LEFT JOIN FETCH t.usuario
        LEFT JOIN FETCH t.adminAsignado
        WHERE t.id = :id
    """)
    Optional<Ticket> findByIdWithMensajes(@Param("id") String id);

    // Stats para el dashboard admin
    long countByEstado(Ticket.Estado estado);

    // Tickets sin asignar
    long countByAdminAsignadoIsNullAndEstadoNot(Ticket.Estado estado);
}
