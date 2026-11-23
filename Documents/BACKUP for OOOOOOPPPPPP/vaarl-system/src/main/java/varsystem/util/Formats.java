package varsystem.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formats {
    private Formats() { }

    public static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);
    public static final DateTimeFormatter DD = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    public static String fmt(LocalDateTime t) { return t == null ? "-" : t.format(DT); }
}