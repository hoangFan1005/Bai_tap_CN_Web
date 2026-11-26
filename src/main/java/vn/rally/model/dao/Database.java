package vn.rally.model.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Shared connection configuration for the application's DAOs. */
public final class Database {
    private static final String DEFAULT_URL =
            "jdbc:mysql://127.0.0.1:3306/rally_sport?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private Database() {
    }

    public static Connection open() throws SQLException {
        return DriverManager.getConnection(
                setting("rally.db.url", "RALLY_DB_URL", DEFAULT_URL),
                setting("rally.db.user", "RALLY_DB_USER", "root"),
                setting("rally.db.password", "RALLY_DB_PASSWORD", ""));
    }

    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null) {
            value = System.getenv(environment);
        }
        return value == null ? fallback : value;
    }
}
