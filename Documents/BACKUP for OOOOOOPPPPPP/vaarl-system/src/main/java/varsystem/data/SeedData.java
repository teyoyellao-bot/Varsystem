package varsystem.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import varsystem.model.Req;
import varsystem.model.Shop;
import varsystem.model.User;

/** Demo data loaded when the application starts. */
public final class SeedData {
    private SeedData() { }

    public static void seed() {
        DB.mkUser("Admin", "admin@var-system.com", "admin", "admin123", "Admin", "", "", "", "Active", 300);
        DB.mkUser("Mac Mac", "macmac@gmail.com", "macmac", "user123", "User", "0912-345-6789",
                "Caloocan, Balayan, Batangas", "Toyota Vios (2019)", "Active", 40);
        User mgr = DB.mkUser("Shop Manager", "shop@gmail.com", "shop", "shop123", "Shop Manager", "0912-345-6789",
                "Caloocan, Balayan, Batangas", "", "Active", 120);
        mgr.shop = "AutoCare Automotive";
        DB.mkUser("Juan Dela Cruz", "juan.delacruz@email.com", "juan", "pass123", "User", "0917-111-2222", "Caloocan, Balayan", "Toyota Vios", "Active", 4);
        DB.mkUser("Maria Santos", "maria.santos@email.com", "maria", "pass123", "User", "0917-222-3333", "Taguig, Metro Manila", "Honda City", "Active", 6);
        DB.mkUser("Mark Reyes", "mark.reyes@email.com", "mark", "pass123", "User", "0917-333-4444", "Balayan, Batangas", "Mitsubishi Mirage", "Pending", 8);
        DB.mkUser("Ana Lopez", "ana.lopez@email.com", "ana", "pass123", "User", "0917-444-5555", "Nasugbu, Batangas", "Nissan Almera", "Active", 10);
        DB.mkUser("Carlos Santos", "carlos.santos@email.com", "carlos", "pass123", "User", "0917-555-6666", "Lemery, Batangas", "Ford Ranger", "Suspended", 12);

        DB.mkSvc("Flat Tire", "Roadside Assistance", "Quick roadside assistance for punctured tires and tire replacement requests.", 142, true);
        DB.mkSvc("Battery Replacement", "Electrical", "Battery testing, jump start and replacement.", 98, true);
        DB.mkSvc("Oil Change", "Maintenance", "Engine oil and filter change to keep your engine healthy.", 76, true);
        DB.mkSvc("Brake Service", "Safety", "Brake inspection, pad replacement and brake fluid service.", 64, true);
        DB.mkSvc("Engine Repair", "Mechanical", "Engine diagnostics and repair.", 53, true);
        DB.mkSvc("Towing", "Roadside Assistance", "Towing service to the nearest repair shop.", 42, false);

        Shop ac = DB.mkShop("AutoCare Automotive", "Caloocan, Balayan, Batangas", 4.8, 123, 1.2, "0912-345-6789", "Active", .52, .20,
                "Engine Repair", "Flat Tire", "Battery Replacement", "Oil Change", "Brake Service", "AC Repair", "Electrical Repair", "General Maintenance");
        DB.mkShop("J&J Vehicle Repair", "Caloocan, Balayan, Batangas", 4.5, 88, 1.5, "0913-222-3333", "Active", .78, .47,
                "Engine Repair", "Oil Change", "Brake Service", "Flat Tire");
        DB.mkShop("Jeremy Auto Repair", "Caloocan, Balayan, Batangas", 3.0, 21, 2.0, "0914-333-4444", "Active", .62, .86,
                "Flat Tire", "Battery Replacement", "General Maintenance");
        DB.mkShop("Ford Service Center", "Nasugbu, Batangas", 4.2, 64, 12.5, "0915-444-5555", "Active", .30, .35,
                "Engine Repair", "Brake Service", "Oil Change", "Towing");
        DB.mkShop("Balayan Auto Garage", "Balayan, Batangas", 4.0, 40, 3.4, "0916-555-6666", "Active", .88, .72,
                "Flat Tire", "Oil Change", "Battery Replacement");
        DB.mkShop("Happy Auto Repair", "Lemery, Batangas", 3.9, 12, 6.1, "0917-666-7777", "Pending", .25, .70,
                "Oil Change", "Brake Service");
        String[][] mech = {{"Carlo Mendoza", "0912-345-6789"}, {"Mark Santos", "0912-345-6789"}, {"John Reyes", "0912-345-6789"},
                {"Alex Cruz", "0912-345-6789"}, {"Kevin Ramos", "0912-345-6789"}, {"Leo Garcia", "0912-345-6789"}};
        ac.mechs.addAll(Arrays.asList(mech));

        String[][] r = {
                {"VAR-001", "juan.delacruz@email.com", "Juan Dela Cruz", "Toyota Vios", "Flat Tire", "Caloocan, Balayan", "Pending", "0"},
                {"VAR-002", "maria.santos@email.com", "Maria Santos", "Honda City", "Engine Repair", "Taguig, Metro Manila", "Accepted", "1"},
                {"VAR-003", "mark.reyes@email.com", "Mark Reyes", "Mitsubishi Mirage", "Battery Replacement", "Balayan, Batangas", "Completed", "2"},
                {"VAR-004", "ana.lopez@email.com", "Ana Lopez", "Nissan Almera", "Brake Service", "Nasugbu, Batangas", "Pending", "2"},
                {"VAR-005", "carlos.santos@email.com", "Carlos Santos", "Ford Ranger", "Oil Change", "Lemery, Batangas", "Accepted", "3"}};
        for (String[] x : r) {
            Req q = new Req();
            q.id = x[0]; q.uemail = x[1]; q.uname = x[2]; q.vehicle = x[3]; q.problem = x[4]; q.location = x[5];
            q.status = x[6]; q.shop = "AutoCare Automotive"; q.submitted = LocalDateTime.now().minusDays(Integer.parseInt(x[7])).minusHours(1);
            q.travel = 100; q.fee = DB.baseFee(q.problem);
            if (!q.status.equals("Pending")) { q.accepted = q.submitted.plusMinutes(30); q.mechanic = "Carlo Mendoza"; }
            DB.reqs.add(q);
        }
        String today = LocalDate.now().toString();
        String[][] ap = {
                {"", "Jose Cruz", "Vios", "Flat Tire", "AutoCare Automotive", today, "09:00 AM", "Pending", "Carlo Mendoza"},
                {"", "Liza Tan", "City", "Engine Repair", "AutoCare Automotive", today, "10:30 AM", "Confirmed", "Mark Santos"},
                {"", "Paolo Lim", "Mirage", "Battery Replacement", "AutoCare Automotive", today, "01:00 PM", "In Service", "John Reyes"},
                {"", "Nina Go", "Almera", "Brake Service", "AutoCare Automotive", today, "03:30 PM", "Confirmed", "Alex Cruz"}};
        DB.appts.addAll(Arrays.asList(ap));
        DB.addNotification("New user registration: Juan Dela Cruz has registered as a new user.");
        DB.addNotification("New repair shop application: Happy Auto Repair submitted a shop application.");
        DB.addNotification("Shop updated service information: J&J Vehicle Repair updated their services.");
        DB.log("System started");
    }
}