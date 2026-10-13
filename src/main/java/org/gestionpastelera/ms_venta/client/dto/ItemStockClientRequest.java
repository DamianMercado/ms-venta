package org.gestionpastelera.ms_venta.client.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemStockClientRequest {
    private String codigoPastel;
    private Integer cantidad;
}
