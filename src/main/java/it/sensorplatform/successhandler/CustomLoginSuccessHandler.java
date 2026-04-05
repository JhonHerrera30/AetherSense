package it.sensorplatform.successhandler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {


	@Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String projectIdParam = request.getParameter("projectId");
        if (projectIdParam == null || projectIdParam.isBlank()) {
            response.sendRedirect("/login?error=missingProject");
            return;
        }

        if(projectIdParam.equals("SUPERADMIN")) {
            request.getSession().setMaxInactiveInterval(60);
            response.sendRedirect("/success");
            return;
        }
         if(authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().contains("ADMIN"))){
            request.getSession().setMaxInactiveInterval(120);
        }
        
        if(authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().contains("OPERATOR"))){
            request.getSession().setMaxInactiveInterval(300);
        }
        Long projectId = Long.parseLong(projectIdParam);
        String url = "/success?projectId=" + projectId;
        response.sendRedirect(url);
    }
}