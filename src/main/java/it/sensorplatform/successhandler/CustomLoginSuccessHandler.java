package it.sensorplatform.successhandler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import it.sensorplatform.model.Credentials;
import it.sensorplatform.service.CredentialsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private CredentialsService credentialsService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String projectIdParam = request.getParameter("projectId");
        if (projectIdParam == null || projectIdParam.isBlank()) {
            response.sendRedirect("/login?error=missingProject");
            return;
        }

        // Recupera le credenziali dell'utente
        Credentials credentials = credentialsService.getCredentials(authentication.getName());

        // Se ha il 2FA attivo, salva in sessione e redirect a verifica
        if (credentials != null && credentials.isTotpEnabled()) {
            request.getSession().setAttribute("2fa_pending", true);
            request.getSession().setAttribute("2fa_projectId", projectIdParam);
            request.getSession().setAttribute("2fa_authentication", authentication);
            // Invalida l'autenticazione finché non viene verificato il codice
            response.sendRedirect("/2fa/verify?projectId=" + projectIdParam);
            return;
        }

        // Nessun 2FA — flusso normale
        if (projectIdParam.equals("SUPERADMIN")) {
            request.getSession().setMaxInactiveInterval(900);
            response.sendRedirect("/success");
            return;
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN"))) {
            request.getSession().setMaxInactiveInterval(1800);
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("OPERATOR"))) {
            request.getSession().setMaxInactiveInterval(7200);
        }

        Long projectId = Long.parseLong(projectIdParam);
        response.sendRedirect("/success?projectId=" + projectId);
    }
}