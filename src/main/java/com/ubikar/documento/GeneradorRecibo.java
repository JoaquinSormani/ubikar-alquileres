package com.ubikar.documento;

// Emitir Recibo: la forma de generar el comprobante es intercambiable (hoy un PDF propio, mañana otro formato).
public interface GeneradorRecibo {

    String tipoContenido();

    byte[] generar(DatosRecibo datos);
}
