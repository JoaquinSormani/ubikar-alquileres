package com.ubikar.documento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;

public final class Formato {

    private static final String[] MENORES_30 = {
        "CERO", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
        "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE",
        "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS",
        "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {
        "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
        "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};
    private static final String[] MESES = {
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

    private static final Locale AR = Locale.forLanguageTag("es-AR");

    private Formato() {
    }

    public static String letras(long n) {
        if (n < 0 || n >= 1_000_000_000_000L) {
            throw new IllegalArgumentException("Número fuera de rango: " + n);
        }
        if (n == 0) {
            return "CERO";
        }
        StringBuilder sb = new StringBuilder();
        long millones = n / 1_000_000;
        long resto = n % 1_000_000;
        if (millones > 0) {
            sb.append(millones == 1 ? "UN MILLÓN" : apocopado(letras(millones)) + " MILLONES");
        }
        if (resto >= 1000) {
            long miles = resto / 1000;
            separar(sb);
            sb.append(miles == 1 ? "MIL" : apocopado(menorMil(miles)) + " MIL");
        }
        long unidades = resto % 1000;
        if (unidades > 0) {
            separar(sb);
            sb.append(menorMil(unidades));
        }
        return sb.toString();
    }

    public static String letrasApocopado(long n) {
        return apocopado(letras(n));
    }

    public static String montoALetras(BigDecimal monto) {
        BigDecimal m = monto.setScale(2, RoundingMode.HALF_UP);
        long enteros = m.longValue();
        int centavos = m.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        String texto = letras(enteros);
        return centavos > 0 ? texto + String.format(" CON %02d/100", centavos) : texto;
    }

    public static String monto(BigDecimal monto) {
        return String.format(AR, "%,.2f", monto.setScale(2, RoundingMode.HALF_UP));
    }

    public static String fecha(LocalDate f) {
        return f.getDayOfMonth() + " de " + mes(f) + " de " + f.getYear();
    }

    public static String mes(LocalDate f) {
        return MESES[f.getMonthValue() - 1];
    }

    public static String dni(String dni) {
        String digitos = dni == null ? "" : dni.replaceAll("\\D", "");
        if (digitos.length() < 7 || digitos.length() > 8) {
            return dni == null ? "" : dni;
        }
        StringBuilder sb = new StringBuilder(digitos).reverse();
        for (int i = 3; i < sb.length(); i += 4) {
            sb.insert(i, '.');
        }
        return sb.reverse().toString();
    }

    private static String menorMil(long n) {
        if (n == 100) {
            return "CIEN";
        }
        StringBuilder sb = new StringBuilder();
        int centena = (int) (n / 100);
        int resto = (int) (n % 100);
        if (centena > 0) {
            sb.append(CENTENAS[centena]);
        }
        if (resto > 0) {
            separar(sb);
            if (resto < 30) {
                sb.append(MENORES_30[resto]);
            } else {
                sb.append(DECENAS[resto / 10]);
                if (resto % 10 > 0) {
                    sb.append(" Y ").append(MENORES_30[resto % 10]);
                }
            }
        }
        return sb.toString();
    }

    private static String apocopado(String texto) {
        if (texto.endsWith("VEINTIUNO")) {
            return texto.substring(0, texto.length() - 9) + "VEINTIÚN";
        }
        if (texto.endsWith("UNO")) {
            return texto.substring(0, texto.length() - 3) + "UN";
        }
        return texto;
    }

    private static void separar(StringBuilder sb) {
        if (sb.length() > 0) {
            sb.append(' ');
        }
    }
}
