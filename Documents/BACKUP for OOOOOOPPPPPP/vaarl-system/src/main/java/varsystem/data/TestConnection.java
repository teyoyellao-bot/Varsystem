
package varsystem.data;

import java.sql.Connection;

public class TestConnection {

    public static void main(String[] args) {

        try (Connection con = DBConnection.getConnection()) {

            if (con != null) {
                System.out.println("CONNECTED TO POSTGRESQL!");
                System.out.println("Database: " +
                        con.getCatalog());
            } else {
                System.out.println("CONNECTION FAILED!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
