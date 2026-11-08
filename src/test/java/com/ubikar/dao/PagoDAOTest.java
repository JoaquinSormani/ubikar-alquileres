package com.ubikar.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PagoDAOTest {

    @Test
    void laBusquedaSeEnvuelveEnComodinesYSeEscapanLosCaracteresEspeciales() {
        assertEquals("%perez%", PagoDAO.patronContiene("perez"));
        assertEquals("%perez%", PagoDAO.patronContiene("  perez  "));
        // Lo que escribe el usuario se busca tal cual: % y _ no son comodines, y la barra se duplica.
        assertEquals("%100\\%%", PagoDAO.patronContiene("100%"));
        assertEquals("%a\\_b%", PagoDAO.patronContiene("a_b"));
        assertEquals("%a\\\\b%", PagoDAO.patronContiene("a\\b"));
        assertEquals("%' OR 1=1 --%", PagoDAO.patronContiene("' OR 1=1 --"));
    }
}
