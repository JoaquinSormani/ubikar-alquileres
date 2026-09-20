package com.ubikar.documento;

import java.util.Map;

public class GeneradorCompleto extends GeneradorPlantilla {

    @Override
    public String nombre() {
        return "completa";
    }

    @Override
    protected String plantilla() {
        return "/plantillas/contrato_completo.docx";
    }

    @Override
    protected Map<String, Object> marcadores(DatosContrato datos) {
        return Marcadores.armarCompleto(datos);
    }
}
