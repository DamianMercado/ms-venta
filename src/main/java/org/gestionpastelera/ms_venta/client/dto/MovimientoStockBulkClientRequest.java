package org.gestionpastelera.ms_venta.client.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoStockBulkClientRequest {
    private String motivo;
    private List<ItemStockClientRequest> items;
}
