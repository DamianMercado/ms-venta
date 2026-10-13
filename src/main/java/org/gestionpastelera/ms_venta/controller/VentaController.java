package org.gestionpastelera.ms_venta.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gestionpastelera.ms_venta.dto.request.ActualizarEstadoVentaRequest;
import org.gestionpastelera.ms_venta.dto.request.VentaRequest;
import org.gestionpastelera.ms_venta.dto.response.RankingPastelResponse;
import org.gestionpastelera.ms_venta.dto.response.ResumenVentasResponse;
import org.gestionpastelera.ms_venta.dto.response.VentaResponse;
import org.gestionpastelera.ms_venta.enums.EstadoVenta;
import org.gestionpastelera.ms_venta.service.VentaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','VENDEDOR','CAJERO')")
        public ResponseEntity<List<VentaResponse>> listarVentas(
            @RequestParam(required = false) EstadoVenta estado
    ) {
        return ResponseEntity.ok(ventaService.listarVentas(estado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','VENDEDOR','CAJERO')")
        public ResponseEntity<VentaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerVentaPorId(id));
    }

    @GetMapping("/cliente/{rut}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','VENDEDOR','CAJERO') or #rut == authentication.name")
        public ResponseEntity<List<VentaResponse>> obtenerPorCliente(@PathVariable String rut) {
        return ResponseEntity.ok(ventaService.obtenerVentasPorCliente(rut));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','VENDEDOR','CAJERO')")
        public ResponseEntity<VentaResponse> crearVenta(
            @Valid @RequestBody VentaRequest request,
            Authentication authentication
    ) {
        String vendedorRut = authentication != null ? authentication.getName() : "SISTEMA";
        VentaResponse response = ventaService.crearVenta(request, vendedorRut);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR','VENDEDOR','CAJERO')")
        public ResponseEntity<VentaResponse> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoVentaRequest request
    ) {
        return ResponseEntity.ok(ventaService.actualizarEstado(id, request));
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR')")
        public ResponseEntity<VentaResponse> anularVenta(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.anularVenta(id));
    }

    @GetMapping("/estadisticas/diarias")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR')")
        public ResponseEntity<ResumenVentasResponse> obtenerEstadisticasDiarias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(ventaService.obtenerEstadisticasDiarias(fecha));
    }

    @GetMapping("/estadisticas/mensuales")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR')")
        public ResponseEntity<ResumenVentasResponse> obtenerEstadisticasMensuales(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes
    ) {
        return ResponseEntity.ok(ventaService.obtenerEstadisticasMensuales(anio, mes));
    }

    @GetMapping("/estadisticas/ranking")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR_SUPERVISOR')")
        public ResponseEntity<List<RankingPastelResponse>> obtenerRanking(
            @RequestParam(defaultValue = "10") Integer limite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return ResponseEntity.ok(ventaService.obtenerRankingPasteles(limite, desde, hasta));
    }
}
