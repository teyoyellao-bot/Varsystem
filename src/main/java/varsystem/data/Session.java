package varsystem.data;

import varsystem.model.Shop;
import varsystem.model.User;

/** Holds the currently logged-in account (shared by all dashboards). */
public final class Session {
    private Session() { }

    public static User ME;
    public static Shop MYSHOP;

    public static void clear() { ME = null; MYSHOP = null; }
}