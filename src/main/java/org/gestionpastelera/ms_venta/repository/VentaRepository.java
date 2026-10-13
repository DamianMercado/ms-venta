package org.gestionpastelera.ms_venta.repository;

import org.gestionpastelera.ms_venta.enums.EstadoVenta;
import org.gestionpastelera.ms_venta.model.Venta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByEstadoVenta(EstadoVenta estadoVenta);

    List<Venta> findByClienteRutOrderByFechaVentaDesc(String clienteRut);

    @Query("SELECT COUNT(v), COALESCE(SUM(v.totalVenta), 0) FROM Venta v " +
           "WHERE v.estadoVenta != :excluirEstado AND v.fechaVenta >= :inicio AND v.fechaVenta < :fin")
    List<Object[]> obtenerEstadisticasPeriodo(
            @Param("excluirEstado") EstadoVenta excluirEstado,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("SELECT d.codigoPastel, d.nombrePastel, SUM(d.cantidad), SUM(d.subtotal) " +
           "FROM DetalleVenta d " +
           "WHERE d.venta.estadoVenta != :excluirEstado " +
           "AND (:desde IS NULL OR d.venta.fechaVenta >= :desde) " +
           "AND (:hasta IS NULL OR d.venta.fechaVenta <= :hasta) " +
           "GROUP BY d.codigoPastel, d.nombrePastel " +
           "ORDER BY SUM(d.cantidad) DESC")
    List<Object[]> obtenerRankingPasteles(
            @Param("excluirEstado") EstadoVenta excluirEstado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
