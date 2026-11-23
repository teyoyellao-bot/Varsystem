
package varsystem.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import varsystem.data.DB;
import varsystem.data.Session;
import varsystem.model.Req;
import varsystem.model.Shop;

public final class RequestService {

    private RequestService() {}

    public static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr",
        "May", "Jun", "Jul", "Aug",
        "Sep", "Oct", "Nov", "Dec"
    };

    public static List<Req> shopReqs() {

        if (Session.ME == null || Session.ME.shop == null) {
            return List.of();
        }

        return DB.reqs.stream()
            .filter(r -> r != null
                && r.shop != null
                && r.shop.equalsIgnoreCase(Session.ME.shop))
            .collect(Collectors.toList());
    }

    public static long cnt(List<Req> l, String st) {
        return l.stream()
            .filter(r -> r != null
                && st != null
                && st.equals(r.status))
            .count();
    }

    public static Map<String, Long> byProblem(List<Req> l) {

        Map<String, Long> m = l.stream()
            .filter(r -> r != null && r.problem != null)
            .collect(Collectors.groupingBy(
                r -> r.problem,
                Collectors.counting()
            ));

        LinkedHashMap<String, Long> out = new LinkedHashMap<>();

        m.entrySet().stream()
            .sorted((a, b) ->
                Long.compare(b.getValue(), a.getValue()))
            .forEach(e -> out.put(e.getKey(), e.getValue()));

        return out;
    }

    public static double[] monthly(double[] base, long cur) {
        double[] d = Arrays.copyOf(base, base.length + 1);
        d[base.length] = cur;
        return d;
    }

    public static double[] monthlyRequests(
            List<Req> requests, int year) {

        double[] monthlyData = new double[12];

        if (requests == null) return monthlyData;

        for (Req r : requests) {

            if (r == null || r.submitted == null) continue;
            if (r.submitted.getYear() != year) continue;

            monthlyData[r.submitted.getMonthValue() - 1]++;
        }

        return monthlyData;
    }

    public static long thisMonth(List<Req> l) {

        if (l == null) return 0;

        return l.stream()
            .filter(r -> r != null
                && r.submitted != null
                && r.submitted.getMonth() == LocalDate.now().getMonth()
                && r.submitted.getYear() == LocalDate.now().getYear())
            .count();
    }

    // ACCEPT REQUEST
    public static void acceptReq(Req r) {

        if (r == null) return;

        if (!"Pending".equalsIgnoreCase(r.status)) return;

        r.status = "Accepted";
        r.accepted = LocalDateTime.now();

        Shop s = Session.MYSHOP;

        // Preserve a mechanic explicitly selected by the Shop Manager.
        // If none was selected, keep legacy automatic assignment behavior.
        if (r.mechanic == null || r.mechanic.isBlank()) {
            if (s != null && s.mechs != null && !s.mechs.isEmpty()) {
                long n = DB.reqs.stream()
                    .filter(x -> x != null && s.name != null
                        && s.name.equalsIgnoreCase(x.shop)
                        && x.mechanic != null && !x.mechanic.isEmpty())
                    .count();
                r.mechanic = s.mechs.get((int) (n % s.mechs.size()))[0];
            } else {
                r.mechanic = "Shop Mechanic";
            }
        }

        // USER NOTIFICATION
        NotificationService.requestAccepted(r);

        // MECHANIC NOTIFICATION
        NotificationService.mechanicAssigned(r);

        DB.log("Request accepted: " + r.id);

        DB.saveRequests();
        DB.saveData();
    }

    // COMPLETE REQUEST
    public static void completeReq(Req r) {

        if (r == null ||
                "Completed".equalsIgnoreCase(r.status)) {
            return;
        }

        r.status = "Completed";
        r.completed = LocalDateTime.now();

        NotificationService.serviceCompleted(r);

        DB.log("Request completed: " + r.id);

        DB.saveRequests();
        DB.saveData();
    }
}
