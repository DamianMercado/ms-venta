package org.gestionpastelera.ms_venta.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingPastelResponse {

        private String codigoPastel;

        private String nombrePastel;

        private Long cantidadVendida;

        private BigDecimal totalVendido;
}
