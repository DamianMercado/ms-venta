CREATE TABLE venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_venta DATETIME NOT NULL,
    cliente_rut VARCHAR(12) NOT NULL,
    cliente_nombre VARCHAR(150) NOT NULL,
    vendedor_rut VARCHAR(12) NOT NULL,
    total_venta DECIMAL(12,2) NOT NULL,
    estado_venta VARCHAR(20) NOT NULL,
    notas VARCHAR(500)
);

CREATE INDEX idx_venta_cliente_rut ON venta (cliente_rut);
CREATE INDEX idx_venta_fecha ON venta (fecha_venta);
CREATE INDEX idx_venta_estado ON venta (estado_venta);

CREATE TABLE detalle_venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    codigo_pastel VARCHAR(30) NOT NULL,
    nombre_pastel VARCHAR(120) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_detalle_venta_venta FOREIGN KEY (venta_id) REFERENCES venta(id) ON DELETE CASCADE
);

CREATE INDEX idx_detalle_venta_venta_id ON detalle_venta (venta_id);
