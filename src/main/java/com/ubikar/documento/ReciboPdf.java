package com.ubikar.documento;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ubikar.modelo.MedioPago;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Recibo de pago de alquiler en PDF. Es un comprobante propio de UBIKAR: no es una factura ni un
 * comprobante fiscal electrónico, y así lo aclara el pie del documento.
 */
public class ReciboPdf implements GeneradorRecibo {

    private static final Color TERRACOTA = new Color(0x60, 0x2b, 0x27);
    private static final Color GRIS = new Color(0x70, 0x65, 0x5f);
    private static final Color LINEA = new Color(0xd9, 0xcf, 0xc4);
    private static final Color FONDO = new Color(0xf6, 0xf1, 0xeb);

    @Override
    public String tipoContenido() {
        return "application/pdf";
    }

    @Override
    public byte[] generar(DatosRecibo d) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document(PageSize.A4, 54, 54, 54, 54);
            PdfWriter.getInstance(documento, salida);
            documento.addTitle("Recibo de pago N° " + numero(d));
            documento.addAuthor("UBIKAR Propiedades");
            documento.open();

            BaseFont base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
            BaseFont negrita = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            Font normal = new Font(base, 10.5f, Font.NORMAL, Color.BLACK);
            Font gris = new Font(base, 9, Font.NORMAL, GRIS);
            Font fuerte = new Font(negrita, 10.5f, Font.NORMAL, Color.BLACK);

            documento.add(cabecera(base, negrita, d));
            documento.add(espacio(14));

            documento.add(datos(normal, fuerte, gris, d));
            documento.add(espacio(14));

            documento.add(detalle(normal, fuerte, d));
            documento.add(espacio(10));

            Paragraph letras = new Paragraph("Son pesos " + Formato.montoALetras(d.total()).toLowerCase() + ".", new Font(base, 9.5f, Font.ITALIC, GRIS));
            documento.add(letras);

            documento.add(espacio(40));
            documento.add(firma(gris));
            documento.add(espacio(22));

            Paragraph pie = new Paragraph("Documento no válido como factura. Comprobante interno de UBIKAR Propiedades.", new Font(base, 8.5f, Font.NORMAL, GRIS));
            pie.setAlignment(Element.ALIGN_CENTER);
            documento.add(pie);

            documento.close();
            return salida.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo generar el recibo", e);
        }
    }

    private static String numero(DatosRecibo d) {
        return String.format("%08d", d.numero());
    }

    private static Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(alto);
        return p;
    }

    private PdfPTable cabecera(BaseFont base, BaseFont negrita, DatosRecibo d) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] {3f, 2f});
        t.setWidthPercentage(100);
        PdfPCell izquierda = new PdfPCell();
        izquierda.setBorder(Rectangle.BOTTOM);
        izquierda.setBorderColor(TERRACOTA);
        izquierda.setBorderWidthBottom(2f);
        izquierda.setPaddingBottom(10);
        izquierda.addElement(new Paragraph("UBIKAR", new Font(negrita, 24, Font.NORMAL, TERRACOTA)));
        izquierda.addElement(new Paragraph("Propiedades · San Luis", new Font(base, 9, Font.NORMAL, GRIS)));
        t.addCell(izquierda);

        PdfPCell derecha = new PdfPCell();
        derecha.setBorder(Rectangle.BOTTOM);
        derecha.setBorderColor(TERRACOTA);
        derecha.setBorderWidthBottom(2f);
        derecha.setPaddingBottom(10);
        Paragraph titulo = new Paragraph("RECIBO DE PAGO", new Font(negrita, 14, Font.NORMAL, Color.BLACK));
        titulo.setAlignment(Element.ALIGN_RIGHT);
        derecha.addElement(titulo);
        Paragraph nro = new Paragraph("N° " + numero(d), new Font(base, 11, Font.NORMAL, Color.BLACK));
        nro.setAlignment(Element.ALIGN_RIGHT);
        derecha.addElement(nro);
        Paragraph fecha = new Paragraph("Fecha: " + Formato.fecha(d.fechaEmision()), new Font(base, 9, Font.NORMAL, GRIS));
        fecha.setAlignment(Element.ALIGN_RIGHT);
        derecha.addElement(fecha);
        t.addCell(derecha);
        return t;
    }

    private PdfPTable datos(Font normal, Font fuerte, Font gris, DatosRecibo d) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] {1.4f, 4f});
        t.setWidthPercentage(100);
        fila(t, "Recibimos de", d.inquilinos(), gris, fuerte);
        fila(t, "Propiedad", d.direccion(), gris, normal);
        fila(t, "Propietario", d.propietario(), gris, normal);
        fila(t, "Período abonado", d.periodo(), gris, fuerte);
        fila(t, "Forma de pago", etiqueta(d.medioPago()), gris, normal);
        fila(t, "Fecha de pago", Formato.fecha(d.fechaPago()), gris, normal);
        return t;
    }

    private static String etiqueta(MedioPago medio) {
        if (medio == MedioPago.TRANSFERENCIA) {
            return "Transferencia";
        }
        return medio == MedioPago.TARJETA_CREDITO ? "Tarjeta de crédito" : "Efectivo";
    }

    private void fila(PdfPTable t, String etiqueta, String valor, Font fe, Font fv) {
        PdfPCell a = new PdfPCell(new Phrase(etiqueta, fe));
        a.setBorder(Rectangle.BOTTOM);
        a.setBorderColor(LINEA);
        a.setPadding(6);
        t.addCell(a);
        PdfPCell b = new PdfPCell(new Phrase(valor, fv));
        b.setBorder(Rectangle.BOTTOM);
        b.setBorderColor(LINEA);
        b.setPadding(6);
        t.addCell(b);
    }

    private PdfPTable detalle(Font normal, Font fuerte, DatosRecibo d) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] {4f, 1.6f});
        t.setWidthPercentage(100);
        encabezado(t, "Concepto", false, fuerte);
        encabezado(t, "Importe", true, fuerte);
        concepto(t, "Alquiler de " + d.periodo() + " (vencía el " + Formato.fecha(d.fechaVencimiento()) + ")", d.alquiler(), normal, false);
        if (d.diasAtraso() > 0) {
            concepto(t, "Interés punitorio por mora: " + d.diasAtraso() + (d.diasAtraso() == 1 ? " día" : " días") + " de atraso (1 % diario)", d.punitorio(), normal, false);
        }
        concepto(t, "TOTAL ABONADO", d.total(), fuerte, true);
        return t;
    }

    private void encabezado(PdfPTable t, String texto, boolean derecha, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setBackgroundColor(FONDO);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(TERRACOTA);
        c.setPadding(7);
        if (derecha) {
            c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        t.addCell(c);
    }

    private void concepto(PdfPTable t, String texto, BigDecimal importe, Font f, boolean total) {
        PdfPCell a = new PdfPCell(new Phrase(texto, f));
        a.setBorder(total ? Rectangle.TOP : Rectangle.BOTTOM);
        a.setBorderColor(total ? TERRACOTA : LINEA);
        a.setPadding(7);
        t.addCell(a);
        PdfPCell b = new PdfPCell(new Phrase("$ " + Formato.monto(importe), f));
        b.setBorder(total ? Rectangle.TOP : Rectangle.BOTTOM);
        b.setBorderColor(total ? TERRACOTA : LINEA);
        b.setPadding(7);
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(b);
    }

    private PdfPTable firma(Font gris) {
        PdfPTable t = new PdfPTable(new float[] {1f, 1f});
        t.setWidthPercentage(100);
        PdfPCell vacia = new PdfPCell(new Phrase(" "));
        vacia.setBorder(Rectangle.NO_BORDER);
        t.addCell(vacia);
        PdfPCell f = new PdfPCell(new Phrase("Firma y sello — UBIKAR Propiedades", gris));
        f.setBorder(Rectangle.TOP);
        f.setBorderColor(GRIS);
        f.setHorizontalAlignment(Element.ALIGN_CENTER);
        f.setPaddingTop(6);
        t.addCell(f);
        return t;
    }
}
