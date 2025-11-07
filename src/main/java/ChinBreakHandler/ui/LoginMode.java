package ChinBreakHandler.ui;

public enum LoginMode {
    MANUAL,
    PROFILES,
    LAUNCHER;

    public static LoginMode parse(String s) {
        if (s == null) {
            return MANUAL; // fallback
        }
        for (LoginMode mode : LoginMode.values()) {
            if (s.equalsIgnoreCase(mode.name())) {
                return mode;
            }
        }
        return MANUAL;
    }
}