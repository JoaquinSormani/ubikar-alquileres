package com.ubikar.documento;

import com.ubikar.modelo.Contrato;
import com.ubikar.modelo.Propiedad;
import com.ubikar.modelo.Propietario;
import java.time.LocalDate;

public record DatosContrato(Contrato contrato, Propietario propietario, Propiedad propiedad, LocalDate fechaFirma) {
}
