package org.gestionpastelera.ms_venta.client;

import org.gestionpastelera.ms_venta.client.dto.MovimientoStockBulkClientRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ms-inventario", url = "${services.inventario.url}")
public interface InventarioClient {

    @PostMapping("/api/v1/inventario/stock/bulk")
    Map<String, Integer> consultarStockBulk(@RequestBody List<String> codigos);

    @PostMapping("/api/v1/inventario/descontar-bulk")
    void descontarStockBulk(@RequestBody MovimientoStockBulkClientRequest request);

    @PostMapping("/api/v1/inventario/reintegrar-bulk")
    void reintegrarStockBulk(@RequestBody MovimientoStockBulkClientRequest request);
}
