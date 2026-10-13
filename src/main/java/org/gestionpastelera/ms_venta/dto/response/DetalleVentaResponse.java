package org.gestionpastelera.ms_venta.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleVentaResponse {

        private Long id;

        private String codigoPastel;

        private String nombrePastel;

        private Integer cantidad;

        private BigDecimal precioUnitario;

        private BigDecimal subtotal;
}
