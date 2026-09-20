package com.ubikar.documento;

// Patron Strategy: cada plantilla de contrato es una estrategia intercambiable.
public interface GeneradorContrato {

    String nombre();

    byte[] generar(DatosContrato datos);
}
