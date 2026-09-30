package com.ubikar.servicio;

import com.ubikar.dao.ContratoDAO;
import com.ubikar.dao.DocumentacionDAO;
import com.ubikar.dao.GaranteDAO;
import com.ubikar.dao.InquilinoDAO;
import com.ubikar.dao.InventarioDAO;
import com.ubikar.dao.PropiedadDAO;
import com.ubikar.dao.PropietarioDAO;
import com.ubikar.dao.RenglonInventarioDAO;
import com.ubikar.dao.Sql2oDAO;
import com.ubikar.documento.DatosContrato;
import com.ubikar.documento.GeneradorCompleto;
import com.ubikar.documento.GeneradorContrato;
import com.ubikar.documento.GeneradorEstandar;
import com.ubikar.modelo.Contrato;
import com.ubikar.modelo.Documentacion;
import com.ubikar.modelo.Garante;
import com.ubikar.modelo.Inquilino;
import com.ubikar.modelo.Inventario;
import com.ubikar.modelo.Persona;
import com.ubikar.modelo.Propiedad;
import com.ubikar.modelo.Propietario;
import com.ubikar.modelo.RenglonInventario;
import com.ubikar.modelo.TipoDocumento;
import com.ubikar.modelo.TipoGarantia;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sql2o.Connection;

public class ContratoServicio {

    private static final Path DIRECTORIO_CONTRATOS = Path.of("documentos", "contratos");
    private static final String PLANTILLA_POR_DEFECTO = "estandar";

    private final PropietarioDAO propietarioDAO = new PropietarioDAO();
    private final PropiedadDAO propiedadDAO = new PropiedadDAO();
    private final InquilinoDAO inquilinoDAO = new InquilinoDAO();
    private final GaranteDAO garanteDAO = new GaranteDAO();
    private final ContratoDAO contratoDAO = new ContratoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final RenglonInventarioDAO renglonDAO = new RenglonInventarioDAO();
    private final DocumentacionDAO documentacionDAO = new DocumentacionDAO();

    private final Map<String, GeneradorContrato> generadores = new LinkedHashMap<>();

    public ContratoServicio() {
        registrarGenerador(new GeneradorEstandar());
        registrarGenerador(new GeneradorCompleto());
    }

    public void registrarGenerador(GeneradorContrato generador) {
        generadores.put(generador.nombre(), generador);
    }

    public Contrato registrar(SolicitudContrato s) {
        resolverPropietarioYPropiedad(s);
        validar(s);
        GeneradorContrato generador = generadores.get(s.getPlantilla() == null ? PLANTILLA_POR_DEFECTO : s.getPlantilla());
        LocalDate fechaFin = s.getFechaInicio().plusMonths(s.getDuracionMeses()).minusDays(1);

        Path archivo = null;
        try (Connection con = Sql2oDAO.getSql2o().beginTransaction()) {
            Propietario propietario = s.getPropietario();
            Propiedad propiedad = s.getPropiedad();

            if (contratoDAO.existeContratoSuperpuesto(propiedad.getId(), s.getFechaInicio(), fechaFin)) {
                throw new IllegalArgumentException("La propiedad ya tiene un contrato vigente en ese período.");
            }

            Contrato c = new Contrato();
            c.setIdPropiedad(propiedad.getId());
            c.setFechaInicio(s.getFechaInicio());
            c.setFechaFin(fechaFin);
            c.setValorInicial(s.getValorInicial());
            c.setPeriodicidadActualizacion(s.getPeriodicidadActualizacion());
            c.setIndiceActualizacion(s.getIndiceActualizacion());
            c.setDestino(s.getDestino());
            c.setDepositoGarantia(s.getDepositoGarantia() != null ? s.getDepositoGarantia() : s.getValorInicial());
            c.setTipoGarantia(s.getTipoGarantia());
            c.setId(contratoDAO.create(con, c));

            for (Inquilino i : s.getInquilinos()) {
                if (i.getId() == null) {
                    i.setId(inquilinoDAO.create(con, i));
                } else {
                    inquilinoDAO.update(con, i);
                }
                contratoDAO.vincularInquilino(con, c.getId(), i.getId());
                c.getInquilinos().add(i);
            }
            for (Garante g : s.getGarantes()) {
                if (g.getId() == null) {
                    g.setId(garanteDAO.create(con, g));
                } else {
                    garanteDAO.update(con, g);
                }
                contratoDAO.vincularGarante(con, c.getId(), g.getId());
                c.getGarantes().add(g);
            }

            Inventario inventario = new Inventario();
            inventario.setIdContrato(c.getId());
            inventario.setFechaRegistro(LocalDate.now());
            inventario.setId(inventarioDAO.create(con, inventario));
            for (RenglonInventario r : s.getRenglones()) {
                r.setIdInventario(inventario.getId());
                r.setId(renglonDAO.create(con, r));
                inventario.getRenglones().add(r);
            }
            c.setInventario(inventario);

            byte[] docx = generador.generar(new DatosContrato(c, propietario, propiedad, LocalDate.now()));
            archivo = DIRECTORIO_CONTRATOS.resolve("contrato-" + c.getId() + ".docx");
            Files.createDirectories(DIRECTORIO_CONTRATOS);
            Files.write(archivo, docx);

            Documentacion doc = new Documentacion();
            doc.setIdContrato(c.getId());
            doc.setTipo(TipoDocumento.CONTRATO);
            doc.setArchivo(archivo.toString().replace('\\', '/'));
            doc.setFechaCarga(LocalDate.now());
            doc.setId(documentacionDAO.create(con, doc));
            c.getDocumentacion().add(doc);

            con.commit(false);
            return c;
        } catch (RuntimeException | IOException e) {
            borrarSilenciosamente(archivo);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("No se pudo guardar el documento del contrato", e);
        }
    }

    public Path documentoDelContrato(Integer idContrato) {
        for (Documentacion d : documentacionDAO.selectByContrato(idContrato)) {
            if (d.getTipo() == TipoDocumento.CONTRATO) {
                return Path.of(d.getArchivo());
            }
        }
        return null;
    }

    // El propietario y la propiedad los da de alta el CU "Registrar Propiedad": aca solo se buscan.
    // Se leen de la base por id y se descartan los datos que haya mandado el cliente.
    private void resolverPropietarioYPropiedad(SolicitudContrato s) {
        requerido(s.getPropietario() != null && s.getPropietario().getId() != null,
                "Falta elegir un propietario registrado.");
        requerido(s.getPropiedad() != null && s.getPropiedad().getId() != null,
                "Falta elegir una propiedad registrada.");
        Propietario propietario = propietarioDAO.read(s.getPropietario().getId());
        requerido(propietario != null, "El propietario indicado no está registrado. Registralo primero desde Registrar Propiedad.");
        Propiedad propiedad = propiedadDAO.read(s.getPropiedad().getId());
        requerido(propiedad != null && propiedad.getIdPropietario().equals(propietario.getId()),
                "La propiedad elegida no pertenece al propietario indicado.");
        s.setPropietario(propietario);
        s.setPropiedad(propiedad);
    }

    private void validar(SolicitudContrato s) {
        validarPersona(s.getPropietario(), "propietario");

        requerido(!s.getInquilinos().isEmpty(), "El contrato necesita al menos un inquilino.");
        for (Inquilino i : s.getInquilinos()) {
            validarPersona(i, "inquilino");
            requerido(!i.getDni().equals(s.getPropietario().getDni()),
                    "El propietario no puede ser inquilino de su propia propiedad.");
        }

        boolean exigeGarante = s.getTipoGarantia() == TipoGarantia.FIADOR_SOLIDARIO
                || s.getTipoGarantia() == TipoGarantia.RECIBO_SUELDO;
        requerido(s.getTipoGarantia() != null, "Falta el tipo de garantía.");
        requerido(!exigeGarante || !s.getGarantes().isEmpty(), "Esa garantía requiere al menos un garante.");
        for (Garante g : s.getGarantes()) {
            validarPersona(g, "garante");
            requerido(s.getInquilinos().stream().noneMatch(i -> i.getDni().equals(g.getDni())),
                    "Un garante no puede ser también inquilino del mismo contrato.");
        }

        requerido(s.getFechaInicio() != null, "Falta la fecha de inicio.");
        requerido(s.getDuracionMeses() != null && s.getDuracionMeses() >= 1, "La duración debe ser de al menos 1 mes.");
        requerido(s.getValorInicial() != null && s.getValorInicial().signum() > 0, "El valor inicial debe ser mayor a cero.");
        requerido(s.getPeriodicidadActualizacion() != null && s.getPeriodicidadActualizacion() >= 1
                && s.getPeriodicidadActualizacion() <= s.getDuracionMeses(),
                "La periodicidad de actualización debe estar entre 1 mes y la duración del contrato.");
        requerido(s.getIndiceActualizacion() != null, "Falta el índice de actualización.");
        requerido(s.getDestino() != null, "Falta el destino del inmueble.");
        requerido(s.getDepositoGarantia() == null || s.getDepositoGarantia().signum() >= 0,
                "El depósito en garantía no puede ser negativo.");

        requerido(!s.getRenglones().isEmpty(), "El inventario necesita al menos un elemento.");
        for (RenglonInventario r : s.getRenglones()) {
            requerido(hayTexto(r.getObjeto()) && r.getEstado() != null,
                    "Cada elemento del inventario necesita un nombre y un estado.");
        }

        String plantilla = s.getPlantilla() == null ? PLANTILLA_POR_DEFECTO : s.getPlantilla();
        requerido(generadores.containsKey(plantilla), "La plantilla '" + plantilla + "' no existe.");
    }

    private void validarPersona(Persona p, String rol) {
        requerido(hayTexto(p.getNombre()) && hayTexto(p.getApellido()), "Faltan el nombre y el apellido del " + rol + ".");
        requerido(hayTexto(p.getDni()) && p.getDni().replaceAll("\\D", "").length() >= 7,
                "El DNI del " + rol + " no es válido.");
        requerido(hayTexto(p.getDomicilio()), "Falta el domicilio del " + rol + " (se usa en el contrato).");
        p.setDni(p.getDni().replaceAll("\\D", ""));
    }

    private static boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static void requerido(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    private static void borrarSilenciosamente(Path archivo) {
        if (archivo != null) {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ignorada) {
                // el archivo huerfano no compromete la consistencia de la base
            }
        }
    }
}
