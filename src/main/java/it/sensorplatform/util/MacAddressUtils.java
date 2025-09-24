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

    public static String formatIdentifierForProject(Number projectId, String macAddress, String devEui) {
        boolean preferMac = projectId != null && projectId.longValue() == 101L;

        String primary = preferMac ? macAddress : devEui;
        String secondary = preferMac ? devEui : macAddress;

        String formattedPrimary = format(primary);
        if (!formattedPrimary.isEmpty()) {
            return formattedPrimary;
        }

        String formattedSecondary = format(secondary);
        return formattedSecondary.isEmpty() ? "--" : formattedSecondary;
    }
}

