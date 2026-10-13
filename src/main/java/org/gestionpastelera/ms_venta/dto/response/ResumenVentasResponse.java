package org.gestionpastelera.ms_venta.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenVentasResponse {

        private String periodo;

        private Long cantidadVentas;

        private BigDecimal totalVendido;

        private BigDecimal ticketPromedio;
}
