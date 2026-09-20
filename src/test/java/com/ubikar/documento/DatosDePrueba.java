package com.ubikar.documento;

import com.ubikar.modelo.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

final class DatosDePrueba {

    private DatosDePrueba() {
    }

    static DatosContrato tipico() {
        Propietario prop = new Propietario();
        prop.setNombre("Maria"); prop.setApellido("Gimenez"); prop.setDni("28555444");
        prop.setDomicilio("Rivadavia 1080"); prop.setEmail("maria@mail.com"); prop.setTelefono("2664007788");
        Inquilino inq = new Inquilino();
        inq.setNombre("Juan"); inq.setApellido("Perez"); inq.setDni("30111222"); inq.setDomicilio("Belgrano 12");
        inq.setTelefono("2664001122"); inq.setEmail("juan@mail.com");
        Garante gar = new Garante();
        gar.setNombre("Ana"); gar.setApellido("Gomez"); gar.setDni("40222333"); gar.setDomicilio("Colon 5");
        gar.setDetalle("recibo de sueldo de Empresa SA, CUIT 30-1, legajo 7");
        Propiedad propiedad = new Propiedad();
        propiedad.setDireccion("Junín 700, Dpto 3");

        Inventario inv = new Inventario();
        inv.getRenglones().add(renglon("Cocina", "Anafe", EstadoObjeto.BUENO, null));
        inv.getRenglones().add(renglon("Cocina", "Mesada", EstadoObjeto.REGULAR, "mancha en el borde"));
        inv.getRenglones().add(renglon("Living", "Ventana", EstadoObjeto.NUEVO, null));

        Contrato c = new Contrato();
        c.setFechaInicio(LocalDate.of(2026, 10, 1));
        c.setFechaFin(LocalDate.of(2027, 9, 30));
        c.setValorInicial(new BigDecimal("185400.50"));
        c.setDepositoGarantia(new BigDecimal("185400.50"));
        c.setPeriodicidadActualizacion(3);
        c.setIndiceActualizacion(IndiceActualizacion.ICL);
        c.setDestino(DestinoInmueble.VIVIENDA);
        c.setTipoGarantia(TipoGarantia.FIADOR_SOLIDARIO);
        c.getInquilinos().add(inq);
        c.getGarantes().add(gar);
        c.setInventario(inv);
        return new DatosContrato(c, prop, propiedad, LocalDate.of(2026, 9, 19));
    }

    static String textoDe(byte[] docx, String nombreArchivo) throws Exception {
        Path salida = Path.of("target", nombreArchivo);
        Files.createDirectories(salida.getParent());
        Files.write(salida, docx);
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx));
                XWPFWordExtractor ext = new XWPFWordExtractor(doc)) {
            return ext.getText();
        }
    }

    private static RenglonInventario renglon(String ambiente, String objeto, EstadoObjeto estado, String obs) {
        RenglonInventario r = new RenglonInventario();
        r.setAmbiente(ambiente); r.setObjeto(objeto); r.setEstado(estado); r.setObservaciones(obs);
        return r;
    }
}
