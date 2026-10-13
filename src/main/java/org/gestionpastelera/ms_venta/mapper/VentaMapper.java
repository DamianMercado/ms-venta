package org.gestionpastelera.ms_venta.mapper;

import org.gestionpastelera.ms_venta.dto.request.DetalleVentaRequest;
import org.gestionpastelera.ms_venta.dto.response.DetalleVentaResponse;
import org.gestionpastelera.ms_venta.dto.response.VentaResponse;
import org.gestionpastelera.ms_venta.model.DetalleVenta;
import org.gestionpastelera.ms_venta.model.Venta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VentaMapper {

    DetalleVentaResponse toDetalleResponse(DetalleVenta detalle);

    List<DetalleVentaResponse> toDetalleResponseList(List<DetalleVenta> detalles);

    @Mapping(target = "items", source = "detalles")
    VentaResponse toResponse(Venta venta);

    List<VentaResponse> toResponseList(List<Venta> ventas);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "venta", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    DetalleVenta toEntity(DetalleVentaRequest request);
}
