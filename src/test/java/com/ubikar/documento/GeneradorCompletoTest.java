package com.ubikar.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ubikar.modelo.Contrato;
import com.ubikar.modelo.IndiceActualizacion;
import com.ubikar.modelo.TipoGarantia;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class GeneradorCompletoTest {

    @Test
    void completaDatosConocidosYDejaEnBlancoLoQueElSistemaNoSabe() throws Exception {
        byte[] docx = new GeneradorCompleto().generar(DatosDePrueba.tipico());
        String texto = DatosDePrueba.textoDe(docx, "contrato-completo-prueba.docx");

        assertFalse(texto.contains("{{"), "quedaron marcadores sin reemplazar");
        assertFalse(Pattern.compile("\\[[A-ZÁÉÍÓÚÑ]").matcher(texto).find(), "quedaron corchetes de la plantilla original");
        assertFalse(texto.contains("SALONIA DORA"), "quedó el locador fijo del modelo");

        // partes
        assertTrue(texto.contains("Entre el/la Sr./Sra. MARIA GIMENEZ, DNI N° 28.555.444, con domicilio en Rivadavia 1080, en adelante el"));
        assertTrue(texto.contains("el/la Sr./Sra. JUAN PEREZ, DNI N° 30.111.222, con domicilio real en Belgrano 12, teléfono N° 2664001122, Mail: juan@mail.com, en adelante denominado"));
        // objeto e inventario
        assertTrue(texto.contains("ubicado en la calle Junín 700, Dpto 3, de la Ciudad de San Luis, Provincia de San Luis."));
        assertTrue(texto.contains("Cocina: Anafe (bueno); Mesada (regular): mancha en el borde."));
        assertTrue(texto.contains("Living: Ventana (nuevo).\n3. Estado de Pintura"), "el inventario debe cortar antes de la cláusula 3");
        // plazo
        assertTrue(texto.contains("DOCE (12) MESES, teniendo como fecha de inicio el día 1 de octubre de 2026"));
        assertTrue(texto.contains("finalizando de pleno derecho"));
        assertTrue(texto.contains("el día 30 de septiembre de 2027"));
        // canon y ajuste
        assertTrue(texto.contains("PESOS CIENTO OCHENTA Y CINCO MIL CUATROCIENTOS CON 50/100 ($185.400,50)"));
        assertTrue(texto.contains("ajustado en forma trimestral mediante la aplicación del índice para Contratos de Locación (ICL)"));
        assertTrue(texto.contains("índice del mes de octubre de 2026 y se lo comparará con el índice del mes de diciembre de 2026"));
        assertTrue(texto.contains("del 1 al 10 de enero de 2027 (para el período que inicia el 1 de enero de 2027)"));
        assertTrue(texto.contains("cada TRES (3) meses"));
        // deposito, garantes, domicilios, cierre
        assertTrue(texto.contains("equivalente a UN (1) MES"));
        assertTrue(texto.contains("1- El/La Sr./Sra. ANA GOMEZ, DNI N° 40.222.333, con domicilio en Colon 5; quien presenta en garantía: recibo de sueldo de Empresa SA, CUIT 30-1, legajo 7."));
        assertTrue(texto.contains("EL LOCADOR: En calle Rivadavia 1080."));
        assertTrue(texto.contains("maria@mail.com | Tel/WhatsApp: 2664007788 EL LOCATARIO"));
        assertTrue(texto.contains("juan@mail.com | Tel/WhatsApp: 2664001122"));
        assertTrue(texto.contains("a los 19 días del mes de septiembre del año 2026"));
        // lo que el sistema no conoce queda como renglon para completar a mano
        assertTrue(texto.contains("__________"));
    }

    @Test
    void sinFiadoresYConDepositoDistintoAlUnMes() throws Exception {
        DatosContrato d = DatosDePrueba.tipico();
        Contrato c = d.contrato();
        c.getGarantes().clear();
        c.setTipoGarantia(TipoGarantia.SEGURO_CAUCION);
        c.setIndiceActualizacion(IndiceActualizacion.IPC);
        c.setPeriodicidadActualizacion(6);
        c.setDepositoGarantia(c.getValorInicial().multiply(new BigDecimal("2")));

        String texto = DatosDePrueba.textoDe(new GeneradorCompleto().generar(d), "contrato-completo-caucion.docx");

        assertFalse(texto.contains("{{"));
        assertTrue(texto.contains("no se constituyen fiadores personales; la garantía ofrecida es: seguro de caución"));
        assertTrue(texto.contains("ajustado en forma semestral mediante la aplicación del Índice de Precios al Consumidor (IPC)"));
        assertTrue(texto.contains("equivalente a DOS (2) MESES"));
        assertTrue(texto.contains("del 1 al 10 de abril de 2027"));
    }

    @Test
    void formasDePeriodicidadYDeposito() {
        DatosContrato d = DatosDePrueba.tipico();
        d.contrato().setPeriodicidadActualizacion(5);
        d.contrato().setDepositoGarantia(d.contrato().getValorInicial().multiply(new BigDecimal("1.5")));
        var m = Marcadores.armarCompleto(d);
        assertEquals("cada 5 meses", m.get("periodicidadForma"));
        assertEquals("1,50 MESES", m.get("depositoMesesTexto"));
    }
}
