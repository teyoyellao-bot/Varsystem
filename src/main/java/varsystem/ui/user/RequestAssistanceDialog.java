
package varsystem.ui.user;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import varsystem.data.DB;
import static varsystem.data.Session.ME;
import varsystem.model.Req;
import varsystem.model.Shop;
import varsystem.model.Svc;
import varsystem.service.LocationService;
import varsystem.service.NotificationService;
import static varsystem.ui.components.Dialogs.err;
import static varsystem.ui.components.Dialogs.msg;
import static varsystem.ui.components.Ui.btn;
import static varsystem.ui.frame.MainFrame.APP;
import static varsystem.ui.frame.MainFrame.go;
import static varsystem.ui.frame.MainFrame.refresh;
import static varsystem.ui.theme.Theme.BLUE;

public final class RequestAssistanceDialog {

    private static final Object REQS_LOCK = DB.reqs;

    private RequestAssistanceDialog() {
    }

    // =========================================================
    // ACTIVE SERVICES
    // =========================================================

    static String[] problemTypes() {

        List<String> services = new ArrayList<>();

        if (DB.svcs != null) {
            services = DB.svcs.stream()
                    .filter(x -> x != null && x.active)
                    .map(x -> x.name)
                    .filter(x -> x != null && !x.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        }

        if (!services.contains("Other")) {
            services.add("Other");
        }

        return services.toArray(new String[0]);
    }

    // =========================================================
    // ACTIVE REQUEST CHECK
    // =========================================================

    private static boolean isActive(Req r) {

        return r != null && r.status != null
                && (r.status.equalsIgnoreCase("Pending")
                || r.status.equalsIgnoreCase("Accepted")
                || r.status.equalsIgnoreCase("On Going"));
    }

    // =========================================================
    // FIND ACTIVE SERVICE
    // =========================================================

    private static Svc findActiveService(String problem) {

        if (problem == null || DB.svcs == null) {
            return null;
        }

        for (Svc service : DB.svcs) {

            if (service == null || !service.active
                    || service.name == null) {
                continue;
            }

            if (service.name.equalsIgnoreCase(problem.trim())) {
                return service;
            }
        }

        return null;
    }

    // =========================================================
    // SERVICE BASE PRICE
    // =========================================================

    private static int feeAmount(String problem) {

        Svc service = findActiveService(problem);

        return service == null
                ? 0
                : Math.max(0, service.base);
    }

    // =========================================================
    // TRAVEL FEE SETTINGS
    // =========================================================

    private static int baseTravelFee() {
        return Math.max(0,
                DB.intS("Service Rules.Base Travel Fee (PHP)", 55));
    }

    private static int ratePerKm() {
        return Math.max(0,
                DB.intS("Service Rules.Rate Per Kilometer (PHP)", 15));
    }

    private static int minimumTravelFee() {
        return Math.max(0,
                DB.intS("Service Rules.Minimum Travel Fee (PHP)", 55));
    }

    // =========================================================
    // VALID COORDINATES
    // =========================================================

    private static boolean validCoordinates(double latitude,
            double longitude) {

        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    // =========================================================
    // DISTANCE CALCULATION - HAVERSINE
    // =========================================================

    private static double distanceKm(
            double lat1, double lon1,
            double lat2, double lon2) {

        double earthRadius = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a));

        return earthRadius * c;
    }

    // =========================================================
    // CALCULATED TRAVEL FEE
    // =========================================================

    private static int calculateTravelFee(double distanceKm) {

        int base = baseTravelFee();
        int rate = ratePerKm();
        int minimum = minimumTravelFee();

        if (!Double.isFinite(distanceKm) || distanceKm < 0) {
            return minimum;
        }

        double computed = base + (distanceKm * rate);

        int calculated = (int) Math.round(computed);

        return Math.max(minimum, calculated);
    }

    // =========================================================
    // DISTANCE BETWEEN USER AND SHOP
    // =========================================================

    private static double getDistance(
            Shop shop,
            double userLatitude,
            double userLongitude) {

        if (shop == null || !shop.hasLocation()
                || !validCoordinates(userLatitude, userLongitude)) {
            return Double.NaN;
        }

        if (!validCoordinates(shop.latitude, shop.longitude)) {
            return Double.NaN;
        }

        return distanceKm(
                userLatitude,
                userLongitude,
                shop.latitude,
                shop.longitude);
    }

    // =========================================================
    // TRAVEL FEE BASED ON ACTUAL DISTANCE
    // =========================================================

    private static int travelAmount(
            Shop shop,
            double userLatitude,
            double userLongitude) {

        double distance = getDistance(
                shop,
                userLatitude,
                userLongitude);

        if (!Double.isFinite(distance)) {
            return minimumTravelFee();
        }

        return calculateTravelFee(distance);
    }

    // =========================================================
    // MECHANIC AVAILABILITY
    // =========================================================

    private static boolean isMechanicAvailable(
            Shop shop,
            String mechanic,
            Req current) {

        if (shop == null || mechanic == null || shop.name == null) {
            return false;
        }

        synchronized (REQS_LOCK) {

            for (Req r : DB.reqs) {

                if (r == null || r == current || !isActive(r)) {
                    continue;
                }

                if (r.shop != null
                        && r.shop.equalsIgnoreCase(shop.name)
                        && r.mechanic != null
                        && r.mechanic.equalsIgnoreCase(mechanic)) {
                    return false;
                }
            }
        }

        return true;
    }

    // =========================================================
    // AVAILABLE MECHANICS
    // =========================================================

    private static List<String> availableMechanics(
            Shop shop,
            Req current) {

        List<String> available = new ArrayList<>();

        if (shop == null || shop.mechs == null) {
            return available;
        }

        for (String[] m : shop.mechs) {

            if (m == null || m.length == 0
                    || m[0] == null || m[0].trim().isEmpty()) {
                continue;
            }

            String name = m[0].trim();

            if (isMechanicAvailable(shop, name, current)
                    && !available.contains(name)) {
                available.add(name);
            }
        }

        return available;
    }

    // =========================================================
    // ORIGINAL METHOD
    // =========================================================

    public static void show(Shop shop, Req ex) {
        show(shop, ex, null);
    }

    // =========================================================
    // METHOD WITH PRESELECTED SERVICE
    // =========================================================

    public static void show(
            Shop shop,
            Req ex,
            String presetService) {

        boolean upd = ex != null;

        if (shop == null) {
            err("No repair shop selected.");
            return;
        }

        var user = ME;

        if (user == null) {
            err("Please log in first.");
            return;
        }

        String userVehicle = user.vehicle != null
                ? user.vehicle : "";

        String userLocation = user.location != null
                ? user.location : "";

        String userEmail = user.email != null
                ? user.email : "";

        String userName = user.name != null
                ? user.name : "";

        List<String> mechanics = availableMechanics(shop, ex);

        // KEEP CURRENT MECHANIC WHEN EDITING
        if (upd && ex.mechanic != null
                && !ex.mechanic.isBlank()
                && !mechanics.contains(ex.mechanic)) {

            mechanics.add(ex.mechanic);
        }

        if (mechanics.isEmpty()) {
            err("No available mechanics in this shop right now.");
            return;
        }

        // =====================================================
        // VEHICLE
        // =====================================================

        JTextField veh = new JTextField(
                ex != null && ex.vehicle != null
                        ? ex.vehicle : userVehicle,
                20);

        // =====================================================
        // PROBLEM TYPE
        // =====================================================

        JComboBox<String> prob = new JComboBox<>(problemTypes());

        if (ex != null && ex.problem != null) {

            boolean exists = false;

            for (int i = 0; i < prob.getItemCount(); i++) {

                if (prob.getItemAt(i).equalsIgnoreCase(ex.problem)) {
                    prob.setSelectedIndex(i);
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                prob.addItem(ex.problem);
                prob.setSelectedItem(ex.problem);
            }

        } else if (presetService != null
                && !presetService.trim().isEmpty()) {

            String service = presetService.trim();

            Svc activeService = findActiveService(service);

            if (activeService == null) {
                err("This service is no longer available.");
                return;
            }

            prob.setSelectedItem(activeService.name);
            prob.setEnabled(false);
        }

        // =====================================================
        // LOCATION
        // =====================================================

        JTextField loc = new JTextField(
                ex != null && ex.location != null
                        ? ex.location : userLocation,
                20);

        boolean locationPreferenceEnabled = user.loc;

        double[] capturedCoordinates = new double[]{
            locationPreferenceEnabled
                    && ex != null
                    && ex.hasUserLocation()
                    ? ex.userLatitude
                    : Double.NaN,

            locationPreferenceEnabled
                    && ex != null
                    && ex.hasUserLocation()
                    ? ex.userLongitude
                    : Double.NaN
        };

        JCheckBox shareLocation = new JCheckBox(
                "Share my current location with this shop only",
                locationPreferenceEnabled
                        && ex != null
                        && ex.shareLocation
                        && ex.hasUserLocation());

        shareLocation.setEnabled(locationPreferenceEnabled);

        if (!locationPreferenceEnabled) {

            shareLocation.setSelected(false);

            shareLocation.setToolTipText(
                    "Enable Location Sharing in Preferences to share coordinates.");
        }

        // =====================================================
        // MECHANIC
        // =====================================================

        JComboBox<String> mech = new JComboBox<>(
                mechanics.toArray(new String[0]));

        if (ex != null && ex.mechanic != null) {
            mech.setSelectedItem(ex.mechanic);
        }

        // =====================================================
        // PAYMENT
        // =====================================================

        JComboBox<String> pay = new JComboBox<>(
                new String[]{"Cash", "GCash", "Cash / GCash"});

        if (ex != null && ex.payment != null) {
            pay.setSelectedItem(ex.payment);
        }

        // =====================================================
        // DETAILS
        // =====================================================

        JTextArea det = new JTextArea(
                ex != null && ex.details != null
                        ? ex.details : "",
                4,
                20);

        det.setLineWrap(true);
        det.setWrapStyleWord(true);

        // =====================================================
        // COST ESTIMATE
        // =====================================================

        JLabel basePriceLabel = new JLabel();
        JLabel travelPriceLabel = new JLabel();
        JLabel distanceLabel = new JLabel();
        JLabel totalPriceLabel = new JLabel();

        Runnable updateEstimate = () -> {

            String selected = (String) prob.getSelectedItem();

            int base = feeAmount(selected);

            double distance = getDistance(
                    shop,
                    capturedCoordinates[0],
                    capturedCoordinates[1]);

            int travel = calculateTravelFee(distance);

            int total = base + travel;

            basePriceLabel.setText(
                    "Base Price: PHP "
                    + String.format("%,d", base));

            travelPriceLabel.setText(
                    "Travel Fee: PHP "
                    + String.format("%,d", travel));

            if (Double.isFinite(distance)) {

                distanceLabel.setText(
                        String.format(
                                "Estimated shop distance: %.2f km",
                                distance));

            } else {

                distanceLabel.setText(
                        "Distance unavailable - minimum travel fee applied.");
            }

            totalPriceLabel.setText(
                    "Estimated Total: PHP "
                    + String.format("%,d", total));
        };

        prob.addActionListener(e -> updateEstimate.run());

        JPanel estimatePanel = new JPanel(new GridBagLayout());

        estimatePanel.setBorder(
                BorderFactory.createTitledBorder("Estimated Cost"));

        GridBagConstraints ec = new GridBagConstraints();

        ec.gridx = 0;
        ec.gridy = 0;
        ec.anchor = GridBagConstraints.WEST;
        ec.insets = new Insets(3, 5, 3, 5);

        estimatePanel.add(basePriceLabel, ec);

        ec.gridy++;
        estimatePanel.add(travelPriceLabel, ec);

        ec.gridy++;
        estimatePanel.add(distanceLabel, ec);

        ec.gridy++;
        estimatePanel.add(totalPriceLabel, ec);

        updateEstimate.run();

        // =====================================================
        // GPS BUTTON
        // =====================================================

        JButton[] curButton = new JButton[1];

        curButton[0] = btn(
                "Get Current GPS Location",
                BLUE,
                e -> {

                    // CHECK LOCATION PREFERENCE
                    if (varsystem.data.Session.ME != null
                            && !varsystem.data.Session.ME.loc) {

                        JOptionPane.showMessageDialog(
                                SwingUtilities.getWindowAncestor(curButton[0]),
                                "Location Sharing is turned OFF in your Preferences.\n"
                                + "Turn it ON first to capture your current location.",
                                "Location Sharing Disabled",
                                JOptionPane.INFORMATION_MESSAGE);

                        return;
                    }

                    curButton[0].setEnabled(false);

                    curButton[0].setText("Getting location...");

                    new SwingWorker<LocationService.Location, Void>() {

                        @Override
                        protected LocationService.Location doInBackground()
                                throws Exception {

                            return LocationService.getCurrentLocation();
                        }

                        @Override
                        protected void done() {

                            curButton[0].setEnabled(true);

                            curButton[0].setText(
                                    "Get Current GPS Location");

                            try {

                                LocationService.Location current = get();

                                capturedCoordinates[0] = current.latitude;
                                capturedCoordinates[1] = current.longitude;

                                loc.setText(current.displayName());

                                shareLocation.setSelected(true);

                                // UPDATE DISTANCE AND TRAVEL FEE
                                updateEstimate.run();

                                msg(
                                        "Current location captured. It will be shared only with "
                                        + shop.name
                                        + " for this active request.");

                            } catch (Exception exLocation) {

                                Throwable cause = exLocation.getCause() == null
                                        ? exLocation
                                        : exLocation.getCause();

                                err(
                                        "Unable to get your device location. "
                                        + "Please enable Windows Location Services "
                                        + "and allow this app to access your location.\n\n"
                                        + cause.getMessage());
                            }
                        }
                    }.execute();
                });

        // =====================================================
        // FORM PANEL
        // =====================================================

        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.gridy = 0;

        p.add(
                new JLabel(
                        upd
                                ? "Update your request"
                                : "Request vehicle assistance from "
                                        + shop.name),
                c);

        c.gridwidth = 1;

        String[] lb = {
            "Vehicle type",
            "Problem type",
            "Your location",
            "Get GPS location",
            "Location sharing",
            "Available mechanic",
            "Payment",
            "Additional details",
            "Cost estimate"
        };

        JComponent[] f = {
            veh,
            prob,
            loc,
            curButton[0],
            shareLocation,
            mech,
            pay,
            new JScrollPane(det),
            estimatePanel
        };

        for (int i = 0; i < lb.length; i++) {

            c.gridy = i + 1;
            c.gridx = 0;
            c.weightx = 0;

            p.add(new JLabel(lb[i]), c);

            c.gridx = 1;
            c.weightx = 1;

            p.add(f[i], c);
        }

        // =====================================================
        // SHOW DIALOG
        // =====================================================

        int result = JOptionPane.showConfirmDialog(
                APP,
                p,
                upd
                        ? "Update Request"
                        : "Request Vehicle Assistance",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        // =====================================================
        // VALIDATION
        // =====================================================

        if (veh.getText().trim().isEmpty()
                || loc.getText().trim().isEmpty()) {

            err("Vehicle type and location are required.");
            return;
        }

        if (shareLocation.isSelected()
                && (!validCoordinates(capturedCoordinates[0],
                        capturedCoordinates[1]))) {

            err("Please get your current location first, "
                    + "or turn off Location Sharing for this request.");

            return;
        }

        // LOCATION SHARING OFF
        if (!user.loc || !shareLocation.isSelected()) {

            capturedCoordinates[0] = Double.NaN;
            capturedCoordinates[1] = Double.NaN;
        }

        String selectedProblem = (String) prob.getSelectedItem();
        String selectedPayment = (String) pay.getSelectedItem();
        String selectedMechanic = (String) mech.getSelectedItem();

        if (selectedProblem == null
                || selectedPayment == null
                || selectedMechanic == null) {

            err("Please complete all required fields.");
            return;
        }

        // =====================================================
        // CHECK ACTIVE SERVICE
        // =====================================================

        if (!selectedProblem.equalsIgnoreCase("Other")
                && findActiveService(selectedProblem) == null) {

            err("This service is no longer active. "
                    + "Please select another service.");

            return;
        }

        // =====================================================
        // FINAL COST CALCULATION
        // =====================================================

        int currentFee = feeAmount(selectedProblem);

        double finalDistance = getDistance(
                shop,
                capturedCoordinates[0],
                capturedCoordinates[1]);

        int currentTravel = calculateTravelFee(finalDistance);

        // =====================================================
        // SAVE REQUEST
        // =====================================================

        synchronized (REQS_LOCK) {

            // CHECK MECHANIC AGAIN BEFORE SAVING
            if (!isMechanicAvailable(
                    shop,
                    selectedMechanic,
                    ex)) {

                err("This mechanic is no longer available. "
                        + "Please select another mechanic.");

                return;
            }

            // =================================================
            // UPDATE EXISTING REQUEST
            // =================================================

            if (ex != null) {

                String oldVehicle = ex.vehicle;
                String oldProblem = ex.problem;
                String oldLocation = ex.location;
                String oldMechanic = ex.mechanic;
                String oldPayment = ex.payment;
                String oldDetails = ex.details;

                double oldLatitude = ex.userLatitude;
                double oldLongitude = ex.userLongitude;

                boolean oldShareLocation = ex.shareLocation;

                int oldFee = ex.fee;
                int oldTravel = ex.travel;

                ex.vehicle = veh.getText().trim();
                ex.problem = selectedProblem;
                ex.location = loc.getText().trim();
                ex.mechanic = selectedMechanic;
                ex.payment = selectedPayment;
                ex.details = det.getText().trim();

                ex.userLatitude = capturedCoordinates[0];
                ex.userLongitude = capturedCoordinates[1];

                ex.shareLocation = shareLocation.isSelected();

                ex.fee = currentFee;
                ex.travel = currentTravel;

                DB.log("Request updated: " + ex.id);

                if (!DB.saveRequests()) {

                    ex.vehicle = oldVehicle;
                    ex.problem = oldProblem;
                    ex.location = oldLocation;
                    ex.mechanic = oldMechanic;
                    ex.payment = oldPayment;
                    ex.details = oldDetails;

                    ex.userLatitude = oldLatitude;
                    ex.userLongitude = oldLongitude;

                    ex.shareLocation = oldShareLocation;

                    ex.fee = oldFee;
                    ex.travel = oldTravel;

                    err("Unable to save your updated request. "
                            + "Please try again.");

                    return;
                }

                DB.saveData();

                msg("Request " + ex.id + " updated and saved.");

                refresh();
                return;
            }

            // =================================================
            // MAXIMUM ACTIVE REQUESTS
            // =================================================

            long active = DB.reqs.stream()
                    .filter(x -> x != null
                            && x.uemail != null
                            && !userEmail.isEmpty()
                            && x.uemail.equalsIgnoreCase(userEmail)
                            && isActive(x))
                    .count();

            if (active >= DB.intS(
                    "Service Rules.Max Active Requests Per User",
                    3)) {

                err("You have reached the maximum number "
                        + "of active requests.");

                return;
            }

            // =================================================
            // CREATE REQUEST
            // =================================================

            Req q = new Req();

            q.id = DB.nextId();
            q.uemail = userEmail;
            q.uname = userName;
            q.vehicle = veh.getText().trim();
            q.problem = selectedProblem;
            q.location = loc.getText().trim();
            q.shop = shop.name;
            q.mechanic = selectedMechanic;
            q.payment = selectedPayment;
            q.details = det.getText().trim();

            q.userLatitude = capturedCoordinates[0];
            q.userLongitude = capturedCoordinates[1];

            q.shareLocation = shareLocation.isSelected();

            // SAVE COMPUTED FEES
            q.fee = currentFee;
            q.travel = currentTravel;

            // ADD REQUEST
            DB.reqs.add(q);

            // SAVE REQUEST FIRST
            if (!DB.saveRequests()) {

                DB.reqs.remove(q);

                err("Your request could not be saved. "
                        + "Please try again.");

                return;
            }

            DB.saveData();

            // UPDATE USER VEHICLE
            if (user.vehicle == null || user.vehicle.isEmpty()) {
                user.vehicle = q.vehicle;
            }

            // DATABASE NOTIFICATION
            NotificationService.requestSubmitted(q);

            // LOCAL SYSTEM NOTIFICATION
            DB.addNotification(
                    "New assistance request: "
                    + userName
                    + " submitted a request for "
                    + q.problem
                    + " to "
                    + shop.name
                    + ". Selected mechanic: "
                    + selectedMechanic);

            // ACTIVITY LOG
            DB.log(
                    "Request submitted: "
                    + q.id
                    + " | Mechanic: "
                    + selectedMechanic);

            // =================================================
            // SUCCESS MESSAGE
            // =================================================

            String distanceText = Double.isFinite(finalDistance)
                    ? String.format("%.2f km", finalDistance)
                    : "Unavailable (minimum fee applied)";

            msg("""
                    Request submitted!
                    Reference ID: %s
                    Shop: %s
                    Mechanic: %s
                    Base Price: PHP %s
                    Distance: %s
                    Travel Fee: PHP %s
                    Estimated Total: PHP %s
                    Status: Pending
                    Your request has been saved.
                    """.formatted(
                    q.id,
                    shop.name,
                    selectedMechanic,
                    String.format("%,d", q.fee),
                    distanceText,
                    String.format("%,d", q.travel),
                    String.format("%,d", q.fee + q.travel)
            ));
        }

        go("My Requests");
    }
}
