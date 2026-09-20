package com.ubikar.documento;

import com.deepoove.poi.XWPFTemplate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

// Base comun de las estrategias que completan una plantilla Word con marcadores {{campo}}.
public abstract class GeneradorPlantilla implements GeneradorContrato {

    protected abstract String plantilla();

    protected abstract Map<String, Object> marcadores(DatosContrato datos);

    @Override
    public byte[] generar(DatosContrato datos) {
        try (InputStream in = getClass().getResourceAsStream(plantilla())) {
            if (in == null) {
                throw new IllegalStateException("No se encontró la plantilla " + plantilla());
            }
            try (XWPFTemplate documento = XWPFTemplate.compile(in).render(marcadores(datos));
                    ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
                documento.write(salida);
                return salida.toByteArray();
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el contrato", e);
        }
    }
}
