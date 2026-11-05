package com.ubikar.dto;

import com.ubikar.modelo.Pago;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Respuesta de Registrar Pago: lo que la pantalla necesita saber del pago recién registrado. El controlador
 * devuelve este objeto en lugar de armar un mapa a mano, así el contrato de la API queda escrito en una clase.
 */
public record PagoRegistradoDTO(Integer id, Integer numeroRecibo, Integer idContrato, LocalDate periodo, LocalDate fechaPago,
        BigDecimal monto, Integer diasAtraso, BigDecimal punitorio, BigDecimal total, String recibo) {

    public static PagoRegistradoDTO de(Pago p) {
        return new PagoRegistradoDTO(p.getId(), p.getNumeroRecibo(), p.getIdContrato(), p.getPeriodo(), p.getFecha(),
                p.getMonto(), p.getDiasAtraso(), p.getPunitorio(), p.getMonto().add(p.getPunitorio()),
                "/api/pagos/" + p.getId() + "/recibo");
    }
}
