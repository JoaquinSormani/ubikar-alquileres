package com.ubikar.documento;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeneradorEstandarTest {

    @Test
    void generaContratoSinMarcadoresSinReemplazar() throws Exception {
        byte[] docx = new GeneradorEstandar().generar(DatosDePrueba.tipico());
        String texto = DatosDePrueba.textoDe(docx, "contrato-estandar-prueba.docx");

        assertFalse(texto.contains("{{"), "quedaron marcadores sin reemplazar");
        assertTrue(texto.contains("MARIA GIMENEZ, DNI N° 28.555.444, con domicilio en Rivadavia 1080"));
        assertTrue(texto.contains("JUAN PEREZ, DNI N° 30.111.222, con domicilio real en Belgrano 12"));
        assertTrue(texto.contains("Junín 700, Dpto 3"));
        assertTrue(texto.contains("uso de vivienda"));
        assertTrue(texto.contains("DOCE (12) MESES"));
        assertTrue(texto.contains("el día 1 de octubre de 2026"));
        assertTrue(texto.contains("el día 30 de septiembre de 2027"));
        assertTrue(texto.contains("PESOS CIENTO OCHENTA Y CINCO MIL CUATROCIENTOS CON 50/100 ($185.400,50)"));
        assertTrue(texto.contains("cada TRES (3) MESES conforme al índice para Contratos de Locación (ICL)"));
        assertTrue(texto.contains("equivalente a un mes de alquiler"));
        assertTrue(texto.contains("[X] Fiador solidario"));
        assertTrue(texto.contains("[  ] Seguro de caución"));
        assertTrue(texto.contains("Garantes: ANA GOMEZ"));
        assertTrue(texto.contains("a los 19 días del mes de septiembre de 2026"));
        assertTrue(texto.contains("Cocina: Anafe (bueno); Mesada (regular): mancha en el borde."));
        assertTrue(texto.contains("Living: Ventana (nuevo)."));
    }
}
