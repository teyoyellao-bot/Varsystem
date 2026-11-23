
package varsystem.service;

import varsystem.data.DB;
import varsystem.data.Session;
import varsystem.model.User;

/** Role-based authentication used by every account type. */
public final class AuthService {
    private AuthService() { }

    /** Returns the matching user, or null when the credentials are wrong. */
    public static User authenticate(String id, String pw) {
        if (id == null || pw == null) return null;

        for (User u : DB.users) {
            if (u == null) continue;

            boolean matchesId =
                    (u.email != null && u.email.equalsIgnoreCase(id))
                    || (u.username != null && u.username.equalsIgnoreCase(id));

            if (matchesId && PasswordSecurity.verifyAndUpgrade(u, pw)) {
                return u;
            }
        }

        return null;
    }

    /** Returns an error message if the user may not log in, otherwise null. */
    public static String loginBlocker(User f) {
        if (f.status.equals("Suspended")) return "This account is suspended. Please contact the administrator.";
        if (!f.role.equals("Admin") && DB.S("General.Maintenance Mode", "Off").equals("On"))
            return "The system is under maintenance. Please try again later.";
        return null;
    }

    /** Starts the session for the user (role decides which dashboard opens later). */
    public static void signIn(User f) {
        Session.ME = f;
        Session.MYSHOP = DB.shops.stream().filter(s -> s.name.equals(f.shop)).findFirst().orElse(null);
        DB.log(f.role + " login: " + f.username);
    }
}

