package varsystem;

import javax.swing.SwingUtilities;

import varsystem.data.DB;
import varsystem.data.SeedData;
import varsystem.ui.frame.MainFrame;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                // Load saved data first.
                boolean loaded = DB.loadData();

                // Only load demo data on the first launch.
                if (!loaded) {

                    System.out.println(
                            "First launch: Loading demo data..."
                    );

                    SeedData.seed();

                    DB.saveAllData();

                } else {

                    System.out.println(
                            "Existing data successfully loaded."
                    );
                }

                // Restore locations and shop photos.
                DB.loadLocations();
                DB.loadShopPhotos();

                // Automatically save when the app closes.
                Runtime.getRuntime().addShutdownHook(
                        new Thread(() -> {

                            System.out.println(
                                    "Saving VARSystem data..."
                            );

                            DB.saveAllData();

                            System.out.println(
                                    "VARSystem data saved successfully."
                            );
                        })
                );

                // Start the application.
                MainFrame.start();

            } catch (Exception e) {

                System.err.println(
                        "Error starting VARSystem: " + e.getMessage()
                );

                e.printStackTrace();
            }
        });
    }
}