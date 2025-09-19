package it.sensorplatform.util;

import java.util.Locale;

public final class MacAddressUtils {

    private MacAddressUtils() {
        // Utility class
    }

    public static String normalize(String mac) {
        if (mac == null) {
            return null;
        }
        String cleaned = mac.replaceAll("[^0-9A-Fa-f]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.toUpperCase(Locale.ROOT);
    }

    public static String format(String mac) {
        String cleaned = normalize(mac);
        return cleaned == null ? "" : cleaned.replaceAll("..(?!$)", "$0:");
    }
}

