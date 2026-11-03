package com.ubikar.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ubikar.modelo.MedioPago;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReciboPdfTest {

    private DatosRecibo datos(int diasAtraso, String punitorio) {
        BigDecimal alquiler = new BigDecimal("500000.00");
        BigDecimal p = new BigDecimal(punitorio);
        return new DatosRecibo(12, LocalDate.of(2026, 11, 2), "Claudia Sormani (DNI 16.117.647)", "Pringles 1422",
                "Maria Gimenez", "octubre de 2026", LocalDate.of(2026, 10, 19), LocalDate.of(2026, 11, 2), MedioPago.EFECTIVO,
                alquiler, diasAtraso, p, alquiler.add(p));
    }

    @Test
    void generaUnPdfValido() {
        ReciboPdf generador = new ReciboPdf();
        byte[] pdf = generador.generar(datos(14, "70000.00"));
        assertEquals("application/pdf", generador.tipoContenido());
        assertTrue(pdf.length > 1000, "el PDF no debería estar vacío");
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.ISO_8859_1));
    }

    @Test
    void generaElReciboDeUnPagoConTarjeta() {
        DatosRecibo d = datos(0, "0.00");
        DatosRecibo conTarjeta = new DatosRecibo(d.numero(), d.fechaEmision(), d.inquilinos(), d.direccion(), d.propietario(), d.periodo(),
                d.fechaVencimiento(), d.fechaPago(), MedioPago.TARJETA_CREDITO, d.alquiler(), 0, d.punitorio(), d.total());
        byte[] pdf = new ReciboPdf().generar(conTarjeta);
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.ISO_8859_1));
    }

    @Test
    void tambienGeneraElReciboDeUnPagoEnTermino() {
        byte[] pdf = new ReciboPdf().generar(datos(0, "0.00"));
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.ISO_8859_1));
    }
}
