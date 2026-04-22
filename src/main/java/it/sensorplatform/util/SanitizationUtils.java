package it.sensorplatform.util;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

public class SanitizationUtils {

    // policy che non permette nessun HTML — solo testo puro
    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS);

    /**
     * Sanitizza un input testuale rimuovendo qualsiasi HTML/JavaScript.
     * Restituisce null se l'input è null.
     */
    public static String sanitize(String input) {
        if (input == null)
            return null;
        // rimuove tutto l'HTML e restituisce solo testo puro
        return POLICY.sanitize(input);
    }
}