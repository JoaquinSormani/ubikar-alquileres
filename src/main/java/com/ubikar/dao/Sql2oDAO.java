package com.ubikar.dao;

import org.sql2o.Sql2o;

/**
 * Singleton: unica instancia de Sql2o para toda la aplicacion,
 * reutilizada por todos los *DAO para conectarse a la base UBIKAR.
 */
public class Sql2oDAO {

    protected static Sql2o sql2o;

    // Valores por defecto para desarrollo local; cada integrante puede pisarlos con
    // variables de entorno.
    private static final String URL = System.getenv().getOrDefault("UBIKAR_DB_URL",
            "jdbc:mysql://localhost:3306/ubikar");

    // CADA UNO AGREGA SU CONTRASEÑA PARA CONECTARSE A SQL.
    private static final String USER = System.getenv().getOrDefault("UBIKAR_DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("UBIKAR_DB_PASSWORD", "matu0805");

    public static Sql2o getSql2o() {
        if (sql2o == null) {
            sql2o = new Sql2o(URL, USER, PASSWORD);
        }
        return sql2o;
    }
}
