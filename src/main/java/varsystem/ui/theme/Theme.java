package varsystem.ui.theme;

import java.awt.Color;

/** Colors and light/dark helpers (unchanged from the original design). */
public final class Theme {
    private Theme() { }

    public static final Color NAVY = new Color(0x00, 0x18, 0x3F), NAVY2 = new Color(0x06, 0x1E, 0x45),
            PANEL = new Color(0x0A, 0x2A, 0x5C), BLUE = new Color(0x0D, 0x6E, 0xFD),
            GREEN = new Color(0x1B, 0xC9, 0x8E), ORANGE = new Color(0xFF, 0xA5, 0x1A),
            RED = new Color(0xE5, 0x3E, 0x4F), PURPLE = new Color(0x8B, 0x4B, 0xF0),
            GRAY = new Color(0x7E, 0x92, 0xB2), LIGHT = new Color(0xE3, 0xEF, 0xFF);
    public static boolean DARK;

    public static Color pageBg() { return DARK ? NAVY2 : Color.WHITE; }
    public static Color cardBg() { return DARK ? PANEL : LIGHT; }
    public static Color cardLine() { return DARK ? new Color(0x1E, 0x4A, 0x8C) : new Color(0xB4, 0xD0, 0xF2); }
    public static Color txt() { return DARK ? Color.WHITE : new Color(0x0B, 0x2A, 0x5C); }
    public static Color sub() { return DARK ? new Color(0x9F, 0xB5, 0xD5) : new Color(0x55, 0x6B, 0x8D); }
}