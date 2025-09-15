package it.sensorplatform.util;

public final class MacAddressUtils {

    private MacAddressUtils() {
        // Utility class
    }

    public static String normalize(String mac) {
        return mac == null ? null : mac.replace(":", "").toUpperCase();
    }

    public static String format(String mac) {
        return mac == null ? "" : mac.replaceAll("..(?!$)", "$0:");
    }
}

