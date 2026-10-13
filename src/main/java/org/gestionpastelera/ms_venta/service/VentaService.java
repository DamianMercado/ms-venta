package org.gestionpastelera.ms_venta.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gestionpastelera.ms_venta.client.InventarioClient;
import org.gestionpastelera.ms_venta.client.dto.ItemStockClientRequest;
import org.gestionpastelera.ms_venta.client.dto.MovimientoStockBulkClientRequest;
import org.gestionpastelera.ms_venta.dto.request.ActualizarEstadoVentaRequest;
import org.gestionpastelera.ms_venta.dto.request.DetalleVentaRequest;
import org.gestionpastelera.ms_venta.dto.request.VentaRequest;
import org.gestionpastelera.ms_venta.dto.response.RankingPastelResponse;
import org.gestionpastelera.ms_venta.dto.response.ResumenVentasResponse;
import org.gestionpastelera.ms_venta.dto.response.VentaResponse;
import org.gestionpastelera.ms_venta.enums.EstadoVenta;
import org.gestionpastelera.ms_venta.mapper.VentaMapper;
import org.gestionpastelera.ms_venta.model.DetalleVenta;
import org.gestionpastelera.ms_venta.model.Venta;
import org.gestionpastelera.ms_venta.repository.VentaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaService {

    private final VentaRepository ventaRepository;
    private final VentaMapper ventaMapper;
    private final InventarioClient inventarioClient;

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentas(EstadoVenta estado) {
        List<Venta> ventas;
        if (estado != null) {
            ventas = ventaRepository.findByEstadoVenta(estado);
        } else {
            ventas = ventaRepository.findAll();
        }
        return ventaMapper.toResponseList(ventas);
    }

    @Transactional(readOnly = true)
    public VentaResponse obtenerVentaPorId(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la venta con ID: " + id));
        return ventaMapper.toResponse(venta);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> obtenerVentasPorCliente(String clienteRut) {
        List<Venta> ventas = ventaRepository.findByClienteRutOrderByFechaVentaDesc(clienteRut);
        return ventaMapper.toResponseList(ventas);
    }

    @Transactional
    public VentaResponse crearVenta(VentaRequest request, String vendedorRut) {
        // 1. Agrupar cantidades solicitadas por código para validación y descuento
        Map<String, Integer> cantidadesPorCodigo = new HashMap<>();
        for (DetalleVentaRequest item : request.getItems()) {
            cantidadesPorCodigo.merge(item.getCodigoPastel().trim(), item.getCantidad(), Integer::sum);
        }

        List<String> codigos = new ArrayList<>(cantidadesPorCodigo.keySet());

        // Consultar stock disponible en ms-inventario
        Map<String, Integer> stockMap;
        try {
            stockMap = inventarioClient.consultarStockBulk(codigos);
        } catch (FeignException e) {
            throw translateFeignException(e, "consultar stock");
        }

        for (Map.Entry<String, Integer> entry : cantidadesPorCodigo.entrySet()) {
            String codigo = entry.getKey();
            int solicitado = entry.getValue();
            int disponible = stockMap != null ? stockMap.getOrDefault(codigo, 0) : 0;

            if (solicitado > disponible) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                        "Stock insuficiente para el producto '%s'. Disponible: %d, solicitado: %d",
                        codigo, disponible, solicitado
                ));
            }
        }

        // 2. Calcular subtotales y total general
        BigDecimal totalVenta = BigDecimal.ZERO;
        List<DetalleVenta> detalles = new ArrayList<>();

        for (DetalleVentaRequest item : request.getItems()) {
            BigDecimal subtotal = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            totalVenta = totalVenta.add(subtotal);

            DetalleVenta detalle = DetalleVenta.builder()
                    .codigoPastel(item.getCodigoPastel().trim())
                    .nombrePastel(item.getNombrePastel().trim())
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioUnitario())
                    .subtotal(subtotal)
                    .build();
            detalles.add(detalle);
        }

        Venta venta = Venta.builder()
                .fechaVenta(LocalDateTime.now())
                .clienteRut(request.getClienteRut().trim())
                .clienteNombre(request.getClienteNombre().trim())
                .vendedorRut(vendedorRut)
                .totalVenta(totalVenta)
                .estadoVenta(EstadoVenta.PENDIENTE_PAGO)
                .notas(request.getNotas() != null ? request.getNotas().trim() : null)
                .build();

        for (DetalleVenta det : detalles) {
            venta.addDetalle(det);
        }

        Venta ventaGuardada = ventaRepository.save(venta);

        // 3. Descontar stock en ms-inventario (si falla, la transacción hace rollback de la venta guardada)
        List<ItemStockClientRequest> itemsParaDescontar = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cantidadesPorCodigo.entrySet()) {
            itemsParaDescontar.add(ItemStockClientRequest.builder()
                    .codigoPastel(entry.getKey())
                    .cantidad(entry.getValue())
                    .build());
        }

        MovimientoStockBulkClientRequest movimientoReq = MovimientoStockBulkClientRequest.builder()
                .motivo("VENTA #" + ventaGuardada.getId())
                .items(itemsParaDescontar)
                .build();

        try {
            inventarioClient.descontarStockBulk(movimientoReq);
        } catch (FeignException e) {
            throw translateFeignException(e, "descontar stock");
        }

        log.info("Venta #{} registrada exitosamente por vendedor {}", ventaGuardada.getId(), vendedorRut);
        return ventaMapper.toResponse(ventaGuardada);
    }

    @Transactional
    public VentaResponse actualizarEstado(Long id, ActualizarEstadoVentaRequest request) {
        EstadoVenta nuevoEstado = request.getEstado();

        if (nuevoEstado == EstadoVenta.ANULADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permite anular la venta mediante este endpoint. Use PATCH /api/v1/ventas/{id}/anular");
        }

        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la venta con ID: " + id));

        if (venta.getEstadoVenta() == nuevoEstado) {
            return ventaMapper.toResponse(venta);
        }

        if (venta.getEstadoVenta() == EstadoVenta.ANULADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede modificar una venta que ya se encuentra ANULADA");
        }

        // Transiciones válidas: PENDIENTE_PAGO -> PAGADA o PAGADA -> PENDIENTE_PAGO (por reversión)
        venta.setEstadoVenta(nuevoEstado);
        Venta ventaActualizada = ventaRepository.save(venta);
        log.info("Venta #{} cambió a estado {}", id, nuevoEstado);

        return ventaMapper.toResponse(ventaActualizada);
    }

    @Transactional
    public VentaResponse anularVenta(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la venta con ID: " + id));

        if (venta.getEstadoVenta() != EstadoVenta.PENDIENTE_PAGO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                    "Solo se pueden anular ventas en estado PENDIENTE_PAGO. El estado actual es %s",
                    venta.getEstadoVenta()
            ));
        }

        // Reintegrar stock en ms-inventario
        Map<String, Integer> cantidadesPorCodigo = new HashMap<>();
        for (DetalleVenta det : venta.getDetalles()) {
            cantidadesPorCodigo.merge(det.getCodigoPastel(), det.getCantidad(), Integer::sum);
        }

        List<ItemStockClientRequest> itemsParaReintegrar = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cantidadesPorCodigo.entrySet()) {
            itemsParaReintegrar.add(ItemStockClientRequest.builder()
                    .codigoPastel(entry.getKey())
                    .cantidad(entry.getValue())
                    .build());
        }

        MovimientoStockBulkClientRequest movimientoReq = MovimientoStockBulkClientRequest.builder()
                .motivo("ANULACION VENTA #" + id)
                .items(itemsParaReintegrar)
                .build();

        try {
            inventarioClient.reintegrarStockBulk(movimientoReq);
        } catch (FeignException e) {
            throw translateFeignException(e, "reintegrar stock por anulación");
        }

        venta.setEstadoVenta(EstadoVenta.ANULADA);
        Venta ventaAnulada = ventaRepository.save(venta);
        log.info("Venta #{} anulada con éxito y stock reintegrado en bodega", id);

        return ventaMapper.toResponse(ventaAnulada);
    }

    @Transactional(readOnly = true)
    public ResumenVentasResponse obtenerEstadisticasDiarias(LocalDate fecha) {
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
        LocalDateTime inicio = fechaConsulta.atStartOfDay();
        LocalDateTime fin = fechaConsulta.plusDays(1).atStartOfDay();

        return calcularResumenPeriodo(fechaConsulta.toString(), inicio, fin);
    }

    @Transactional(readOnly = true)
    public ResumenVentasResponse obtenerEstadisticasMensuales(Integer anio, Integer mes) {
        YearMonth ym;
        if (anio != null && mes != null) {
            ym = YearMonth.of(anio, mes);
        } else {
            ym = YearMonth.now();
        }

        LocalDateTime inicio = ym.atDay(1).atStartOfDay();
        LocalDateTime fin = ym.plusMonths(1).atDay(1).atStartOfDay();

        return calcularResumenPeriodo(ym.toString(), inicio, fin);
    }

    @Transactional(readOnly = true)
    public List<RankingPastelResponse> obtenerRankingPasteles(Integer limite, LocalDate desde, LocalDate hasta) {
        int limit = (limite != null && limite > 0) ? limite : 10;
        LocalDateTime fechaDesde = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime fechaHasta = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

        List<Object[]> resultados = ventaRepository.obtenerRankingPasteles(
                EstadoVenta.ANULADA,
                fechaDesde,
                fechaHasta,
                PageRequest.of(0, limit)
        );

        List<RankingPastelResponse> ranking = new ArrayList<>();
        for (Object[] row : resultados) {
            String codigo = (String) row[0];
            String nombre = (String) row[1];
            Long cantidad = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            BigDecimal total = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;

            ranking.add(RankingPastelResponse.builder()
                    .codigoPastel(codigo)
                    .nombrePastel(nombre)
                    .cantidadVendida(cantidad)
                    .totalVendido(total)
                    .build());
        }

        return ranking;
    }

    private ResumenVentasResponse calcularResumenPeriodo(String periodo, LocalDateTime inicio, LocalDateTime fin) {
        List<Object[]> stats = ventaRepository.obtenerEstadisticasPeriodo(EstadoVenta.ANULADA, inicio, fin);
        long cantidadVentas = 0L;
        BigDecimal totalVendido = BigDecimal.ZERO;

        if (stats != null && !stats.isEmpty()) {
            Object[] row = stats.get(0);
            if (row != null) {
                cantidadVentas = row[0] != null ? ((Number) row[0]).longValue() : 0L;
                totalVendido = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            }
        }

        BigDecimal ticketPromedio = cantidadVentas > 0
                ? totalVendido.divide(BigDecimal.valueOf(cantidadVentas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ResumenVentasResponse.builder()
                .periodo(periodo)
                .cantidadVentas(cantidadVentas)
                .totalVendido(totalVendido)
                .ticketPromedio(ticketPromedio)
                .build();
    }

    private RuntimeException translateFeignException(FeignException e, String operacion) {
        int status = e.status();
        if (status == 404) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso no encontrado en servicio de inventario");
        }
        if (status == 400 || status == 409) {
            String body = e.contentUTF8();
            if (body != null) {
                Matcher matcher = MESSAGE_PATTERN.matcher(body);
                if (matcher.find()) {
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, matcher.group(1));
                }
            }
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error en servicio de inventario: " + e.getMessage());
        }
        if (status == 401 || status == 403) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El servicio de inventario rechazó la autorización del usuario actual");
        }
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de inventario no está disponible para " + operacion + ": " + e.getMessage());
    }
}
