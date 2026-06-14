package it.sensorplatform.controller;

import it.sensorplatform.service.TotpService;
import it.sensorplatform.model.Credentials;
import it.sensorplatform.service.CredentialsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/2fa")
public class TwoFactorController {

    @Autowired
    private TotpService totpService;

    @Autowired
    private CredentialsService credentialsService;

    @GetMapping("/verify")
    public String showVerifyPage(@RequestParam(required = false) String projectId,
            HttpSession session, Model model) {
        Boolean pending = (Boolean) session.getAttribute("2fa_pending");
        if (pending == null || !pending) {
            return "redirect:/login";
        }
        model.addAttribute("projectId", projectId);
        return "2fa-verify";
    }

    @PostMapping("/verify")
    public String verifyCode(@RequestParam String code,
            @RequestParam(required = false) String projectId,
            HttpSession session,
            HttpServletRequest request) {

        Boolean pending = (Boolean) session.getAttribute("2fa_pending");
        if (pending == null || !pending) {
            return "redirect:/login";
        }

        Authentication authentication = (Authentication) session.getAttribute("2fa_authentication");
        if (authentication == null) {
            session.invalidate();
            return "redirect:/login?error";
        }

        Credentials credentials = credentialsService.getCredentials(authentication.getName());
        if (credentials == null || !credentials.isTotpEnabled()) {
            session.invalidate();
            return "redirect:/login?error";
        }

        if (!totpService.verifyCode(credentials.getTotpSecret(), code)) {
            session.setAttribute("2fa_error", "Codice non valido. Riprova.");
            return "redirect:/2fa/verify?projectId=" + projectId;
        }

        // Codice valido — completa l'autenticazione
        session.removeAttribute("2fa_pending");
        session.removeAttribute("2fa_authentication");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Imposta timeout sessione
        if (projectId != null && projectId.equals("SUPERADMIN")) {
            session.setMaxInactiveInterval(900);
            return "redirect:/success";
        }

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN"))) {
            session.setMaxInactiveInterval(1800);
        } else {
            session.setMaxInactiveInterval(7200);
        }

        return "redirect:/success?projectId=" + projectId;
    }
}