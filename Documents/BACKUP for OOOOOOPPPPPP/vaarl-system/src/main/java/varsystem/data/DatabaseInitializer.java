
package varsystem.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {

        String[] tables = {
            "users",
            "shops",
            "services",
            "mechanics",
            "shop_services",
            "requests",
            "feedback",
            "appointments",
            "chat_messages",
            "chat_read_positions",
            "notifications",
            "notification_read_counts",
            "maintenance_records",
            "service_requests",
            "support_messages",
            "system_logs",
            "system_settings",
            "vaarl_schema_version"
        };

        try (Connection connection = DBConnection.getConnection()) {

            if (connection == null) {
                throw new SQLException(
                    "Unable to connect to PostgreSQL."
                );
            }

            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                for (String table : tables) {

                    String sql =
                        "SELECT to_regclass(?)";

                    try (PreparedStatement statement =
                            connection.prepareStatement(sql)) {

                        statement.setString(1, "public." + table);

                        try (ResultSet result =
                                statement.executeQuery()) {

                            if (!result.next()
                                    || result.getString(1) == null) {

                                throw new SQLException(
                                    "Missing database table: " + table
                                );
                            }
                        }
                    }
                }

                String versionSql =
                    "INSERT INTO public.vaarl_schema_version(version) " +
                    "SELECT 1 WHERE NOT EXISTS " +
                    "(SELECT 1 FROM public.vaarl_schema_version)";

                try (PreparedStatement statement =
                        connection.prepareStatement(versionSql)) {

                    statement.executeUpdate();
                }

                connection.commit();

                System.out.println(
                    "VAARL PostgreSQL database initialized successfully."
                );

            } catch (SQLException ex) {

                connection.rollback();
                throw ex;

            } finally {

                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public static boolean isConnected() {

        try (Connection connection = DBConnection.getConnection()) {

            return connection != null && connection.isValid(5);

        } catch (SQLException ex) {

            System.err.println(
                "PostgreSQL connection failed: " + ex.getMessage()
            );

            return false;
        }
    }
}
