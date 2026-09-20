package com.ubikar.documento;

import java.util.Map;

public class GeneradorEstandar extends GeneradorPlantilla {

    @Override
    public String nombre() {
        return "estandar";
    }

    @Override
    protected String plantilla() {
        return "/plantillas/contrato_estandar.docx";
    }

    @Override
    protected Map<String, Object> marcadores(DatosContrato datos) {
        return Marcadores.armar(datos);
    }
}
