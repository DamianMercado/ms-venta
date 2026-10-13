package org.gestionpastelera.ms_venta.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.gestionpastelera.ms_venta.enums.EstadoVenta;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarEstadoVentaRequest {

    @NotNull(message = "El estado de la venta es obligatorio")
        private EstadoVenta estado;
}
