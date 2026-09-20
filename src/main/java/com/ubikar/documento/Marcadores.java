package com.ubikar.documento;

import com.ubikar.modelo.Contrato;
import com.ubikar.modelo.Garante;
import com.ubikar.modelo.IndiceActualizacion;
import com.ubikar.modelo.Persona;
import com.ubikar.modelo.RenglonInventario;
import com.ubikar.modelo.TipoGarantia;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Marcadores {

    private Marcadores() {
    }

    public static Map<String, Object> armar(DatosContrato d) {
        Contrato c = d.contrato();
        Map<String, Object> m = new LinkedHashMap<>();

        m.put("locador", persona(d.propietario(), "el/la Sr./Sra.", "con domicilio en"));
        m.put("locatario", locatarios(c.getInquilinos()));
        m.put("direccionInmueble", d.propiedad().getDireccion());
        m.put("destinoTexto", switch (c.getDestino()) {
            case VIVIENDA -> "uso de vivienda";
            case COMERCIAL -> "uso comercial";
        });

        long meses = ChronoUnit.MONTHS.between(c.getFechaInicio(), c.getFechaFin().plusDays(1));
        m.put("plazoTexto", cantidadMeses(meses));
        m.put("fechaInicioTexto", Formato.fecha(c.getFechaInicio()));
        m.put("fechaFinTexto", Formato.fecha(c.getFechaFin()));

        m.put("montoLetras", Formato.montoALetras(c.getValorInicial()));
        m.put("monto", Formato.monto(c.getValorInicial()));
        m.put("periodicidadTexto", cantidadMeses(c.getPeriodicidadActualizacion()));
        m.put("indiceTexto", c.getIndiceActualizacion() == IndiceActualizacion.ICL
                ? "índice para Contratos de Locación (ICL) publicado por el Banco Central de la República Argentina"
                : "Índice de Precios al Consumidor (IPC) publicado por el INDEC");

        m.put("depositoLetras", Formato.montoALetras(c.getDepositoGarantia()));
        m.put("deposito", Formato.monto(c.getDepositoGarantia()));
        m.put("depositoEquivalencia", equivalenciaDeposito(c.getDepositoGarantia(), c.getValorInicial()));

        m.put("gTitulo", casilla(c.getTipoGarantia() == TipoGarantia.TITULO_PROPIEDAD));
        m.put("gCaucion", casilla(c.getTipoGarantia() == TipoGarantia.SEGURO_CAUCION));
        m.put("gFiador", casilla(c.getTipoGarantia() == TipoGarantia.FIADOR_SOLIDARIO));
        m.put("gSueldo", casilla(c.getTipoGarantia() == TipoGarantia.RECIBO_SUELDO));
        m.put("detalleGarantes", detalleGarantes(c.getGarantes()));

        m.put("diaFirma", String.valueOf(d.fechaFirma().getDayOfMonth()));
        m.put("mesFirma", Formato.mes(d.fechaFirma()));
        m.put("anioFirma", String.valueOf(d.fechaFirma().getYear()));

        m.put("inventarioTexto", inventario(c));
        return m;
    }

    public static Map<String, Object> armarCompleto(DatosContrato d) {
        Map<String, Object> m = armar(d);
        Contrato c = d.contrato();
        LocalDate inicio = c.getFechaInicio();
        LocalDate fin = c.getFechaFin();
        int periodicidad = c.getPeriodicidadActualizacion();
        // Primer ajuste: rige desde inicio + periodicidad; base = mes de inicio;
        // indice final = mes inmediato anterior al periodo ajustado.
        LocalDate inicioPeriodo = inicio.plusMonths(periodicidad);
        LocalDate mesIndiceFinal = inicioPeriodo.minusMonths(1);

        m.put("inicioDia", String.valueOf(inicio.getDayOfMonth()));
        m.put("inicioMes", Formato.mes(inicio));
        m.put("inicioAnio", String.valueOf(inicio.getYear()));
        m.put("finDia", String.valueOf(fin.getDayOfMonth()));
        m.put("finMes", Formato.mes(fin));
        m.put("finAnio", String.valueOf(fin.getYear()));

        m.put("periodicidadForma", formaPeriodicidad(periodicidad));
        m.put("periodicidadLetras", Formato.letrasApocopado(periodicidad).toUpperCase(Locale.ROOT));
        m.put("periodicidadNumero", String.valueOf(periodicidad));
        m.put("baseMes", Formato.mes(inicio));
        m.put("baseAnio", String.valueOf(inicio.getYear()));
        m.put("finalMes", Formato.mes(mesIndiceFinal));
        m.put("finalAnio", String.valueOf(mesIndiceFinal.getYear()));
        m.put("pagoDiaInicio", "1");
        m.put("pagoDiaFin", "10");
        m.put("ventanaPago", "1 al 10");
        m.put("pagoMes", Formato.mes(inicioPeriodo));
        m.put("pagoAnio", String.valueOf(inicioPeriodo.getYear()));
        m.put("periodoDia", String.valueOf(inicioPeriodo.getDayOfMonth()));
        m.put("periodoMes", Formato.mes(inicioPeriodo));
        m.put("periodoAnio", String.valueOf(inicioPeriodo.getYear()));

        m.put("inventarioTexto", inventario(c) + "\n");
        m.put("depositoMesesTexto", mesesDeposito(c.getDepositoGarantia(), c.getValorInicial()));

        m.put("locatarioCompleto", locatariosConContacto(c.getInquilinos()));
        m.put("listaGarantes", listaGarantes(c));
        m.put("domicilioLocador", d.propietario().getDomicilio());
        m.put("emailLocador", dato(d.propietario().getEmail()));
        m.put("telefonoLocador", dato(d.propietario().getTelefono()));
        m.put("emailLocatario", contactoInquilinos(c.getInquilinos(), Persona::getEmail));
        m.put("telefonoLocatario", contactoInquilinos(c.getInquilinos(), Persona::getTelefono));
        return m;
    }

    private static String formaPeriodicidad(int meses) {
        return switch (meses) {
            case 1 -> "mensual";
            case 2 -> "bimestral";
            case 3 -> "trimestral";
            case 4 -> "cuatrimestral";
            case 6 -> "semestral";
            case 12 -> "anual";
            default -> "cada " + meses + " meses";
        };
    }

    private static String mesesDeposito(BigDecimal deposito, BigDecimal valor) {
        BigDecimal[] div = deposito.divideAndRemainder(valor);
        if (div[1].signum() == 0 && div[0].signum() > 0) {
            long n = div[0].longValue();
            return n == 1 ? "UN (1) MES" : Formato.letrasApocopado(n) + " (" + n + ") MESES";
        }
        BigDecimal razon = deposito.divide(valor, 2, RoundingMode.HALF_UP);
        return String.format(Locale.forLanguageTag("es-AR"), "%,.2f", razon) + " MESES";
    }

    private static String dato(String valor) {
        return valor == null || valor.isBlank() ? "no informado" : valor.trim();
    }

    private static String contactoInquilinos(List<? extends Persona> inquilinos, java.util.function.Function<Persona, String> campo) {
        List<String> valores = new ArrayList<>();
        for (Persona p : inquilinos) {
            String v = campo.apply(p);
            if (v != null && !v.isBlank()) {
                valores.add(v.trim());
            }
        }
        return valores.isEmpty() ? "no informado" : String.join(" / ", valores);
    }

    private static String contacto(Persona p) {
        StringBuilder sb = new StringBuilder();
        if (p.getTelefono() != null && !p.getTelefono().isBlank()) {
            sb.append(", teléfono N° ").append(p.getTelefono().trim());
        }
        if (p.getEmail() != null && !p.getEmail().isBlank()) {
            sb.append(", Mail: ").append(p.getEmail().trim());
        }
        return sb.toString();
    }

    private static String locatariosConContacto(List<? extends Persona> inquilinos) {
        List<String> partes = new ArrayList<>();
        for (Persona p : inquilinos) {
            partes.add(nombreCompleto(p) + ", DNI N° " + Formato.dni(p.getDni())
                    + ", con domicilio real en " + p.getDomicilio() + contacto(p));
        }
        if (partes.size() == 1) {
            return "el/la Sr./Sra. " + partes.get(0);
        }
        String ultimo = partes.remove(partes.size() - 1);
        return "los Sres. " + String.join("; ", partes) + "; y " + ultimo;
    }

    private static String listaGarantes(Contrato c) {
        if (c.getGarantes().isEmpty()) {
            String garantia = c.getTipoGarantia() == TipoGarantia.SEGURO_CAUCION ? "seguro de caución" : "título de propiedad";
            return "(no se constituyen fiadores personales; la garantía ofrecida es: " + garantia + ").";
        }
        List<String> items = new ArrayList<>();
        int n = 1;
        for (Garante g : c.getGarantes()) {
            String item = n++ + "- El/La Sr./Sra. " + nombreCompleto(g) + ", DNI N° " + Formato.dni(g.getDni())
                    + ", con domicilio en " + g.getDomicilio() + contacto(g);
            if (g.getDetalle() != null && !g.getDetalle().isBlank()) {
                item += "; quien presenta en garantía: " + g.getDetalle().trim();
            }
            items.add(item + ".");
        }
        return String.join(" ", items);
    }

    private static String persona(Persona p, String tratamiento, String conector) {
        return tratamiento + " " + nombreCompleto(p) + ", DNI N° " + Formato.dni(p.getDni())
                + ", " + conector + " " + p.getDomicilio();
    }

    private static String locatarios(List<? extends Persona> inquilinos) {
        if (inquilinos.size() == 1) {
            return persona(inquilinos.get(0), "el/la Sr./Sra.", "con domicilio real en");
        }
        List<String> partes = new ArrayList<>();
        for (Persona p : inquilinos) {
            partes.add(nombreCompleto(p) + ", DNI N° " + Formato.dni(p.getDni())
                    + ", con domicilio real en " + p.getDomicilio());
        }
        String ultimo = partes.remove(partes.size() - 1);
        return "los Sres. " + String.join(", ", partes) + " y " + ultimo;
    }

    private static String nombreCompleto(Persona p) {
        return (p.getNombre() + " " + p.getApellido()).toUpperCase(Locale.ROOT);
    }

    private static String cantidadMeses(long meses) {
        return Formato.letrasApocopado(meses) + " (" + meses + ") " + (meses == 1 ? "MES" : "MESES");
    }

    private static String equivalenciaDeposito(BigDecimal deposito, BigDecimal valor) {
        BigDecimal[] div = deposito.divideAndRemainder(valor);
        if (div[1].signum() == 0 && div[0].signum() > 0) {
            long n = div[0].longValue();
            return (n == 1 ? "un mes" : Formato.letrasApocopado(n).toLowerCase(Locale.ROOT) + " meses") + " de alquiler";
        }
        return "la suma pactada por las partes";
    }

    private static String casilla(boolean marcada) {
        return marcada ? "[X]" : "[  ]";
    }

    private static String detalleGarantes(List<Garante> garantes) {
        if (garantes.isEmpty()) {
            return "";
        }
        List<String> items = new ArrayList<>();
        for (Garante g : garantes) {
            String item = nombreCompleto(g) + ", DNI N° " + Formato.dni(g.getDni()) + ", con domicilio en " + g.getDomicilio();
            if (g.getDetalle() != null && !g.getDetalle().isBlank()) {
                item += " (" + g.getDetalle().trim() + ")";
            }
            items.add(item);
        }
        return "Garantes: " + String.join("; ", items) + ".";
    }

    private static String inventario(Contrato c) {
        if (c.getInventario() == null || c.getInventario().getRenglones().isEmpty()) {
            return "Sin elementos registrados.";
        }
        Map<String, List<String>> porAmbiente = new LinkedHashMap<>();
        for (RenglonInventario r : c.getInventario().getRenglones()) {
            String ambiente = r.getAmbiente() == null || r.getAmbiente().isBlank() ? "General" : r.getAmbiente().trim();
            String linea = r.getObjeto().trim() + " (" + r.getEstado().name().toLowerCase(Locale.ROOT) + ")";
            if (r.getObservaciones() != null && !r.getObservaciones().isBlank()) {
                linea += ": " + r.getObservaciones().trim();
            }
            porAmbiente.computeIfAbsent(ambiente, k -> new ArrayList<>()).add(linea);
        }
        List<String> lineas = new ArrayList<>();
        porAmbiente.forEach((ambiente, items) -> lineas.add(ambiente + ": " + String.join("; ", items) + "."));
        return String.join("\n", lineas);
    }
}
