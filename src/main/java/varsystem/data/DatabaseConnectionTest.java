
package varsystem.data;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class DatabaseConnectionTest {

    public static void main(String[] args) {

        System.out.println("VAARL POSTGRESQL CONNECTION TEST");

        try (Connection connection = DBConnection.getConnection()) {

            if (connection != null && connection.isValid(5)) {
                DatabaseMetaData meta = connection.getMetaData();

                System.out.println("STATUS: CONNECTED SUCCESSFULLY");
                System.out.println("DATABASE: " + meta.getURL());
                System.out.println("USERNAME: " + meta.getUserName());
                System.out.println("VERSION: " + meta.getDatabaseProductVersion());
            }

        } catch (Exception e) {
            System.out.println("STATUS: CONNECTION FAILED");
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
