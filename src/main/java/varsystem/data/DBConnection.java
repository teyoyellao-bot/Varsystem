
package varsystem.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {

    private static final String URL =
            System.getenv().getOrDefault(
                    "VAARL_DB_URL",
                    "jdbc:postgresql://localhost:5432/vaarl_db"
            );

    private static final String USER =
            System.getenv("VAARL_DB_USER");

    private static final String PASSWORD =
            System.getenv("VAARL_DB_PASSWORD");

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {

        if (USER == null || USER.isBlank()) {
            throw new SQLException(
                    "Missing VAARL_DB_USER environment variable."
            );
        }

        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException(
                    "Missing VAARL_DB_PASSWORD environment variable."
            );
        }

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException(
                    "PostgreSQL JDBC Driver not found. Check your JAR library.",
                    ex
            );
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static boolean isConnected() {

        try (Connection connection = getConnection()) {
            return connection != null && connection.isValid(5);

        } catch (SQLException ex) {
            System.err.println(
                    "PostgreSQL connection failed: " + ex.getMessage()
            );
            return false;
        }
    }
}
