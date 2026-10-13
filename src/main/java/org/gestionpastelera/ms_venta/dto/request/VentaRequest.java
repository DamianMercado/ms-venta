package org.gestionpastelera.ms_venta.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaRequest {

    @NotBlank(message = "El RUT del cliente es obligatorio")
    @Pattern(regexp = "^[0-9]{7,8}-[0-9kK]$", message = "El RUT debe tener el formato 12345678-9")
        private String clienteRut;

    @NotBlank(message = "El nombre del cliente es obligatorio")
        private String clienteNombre;

        private String notas;

    @NotEmpty(message = "La venta debe incluir al menos un ítem")
    @Valid
        private List<DetalleVentaRequest> items;
}
