package org.gestionpastelera.ms_venta.dto.response;

import lombok.*;
import org.gestionpastelera.ms_venta.enums.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaResponse {

        private Long id;

        private LocalDateTime fechaVenta;

        private String clienteRut;

        private String clienteNombre;

        private String vendedorRut;

        private BigDecimal totalVenta;

        private EstadoVenta estadoVenta;

        private String notas;

        private List<DetalleVentaResponse> items;
}
