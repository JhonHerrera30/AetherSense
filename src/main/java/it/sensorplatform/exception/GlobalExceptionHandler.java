package it.sensorplatform.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonErrors(
            HttpMessageNotReadableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "Payload non valido o formattazione errata.");
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericErrors(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path.equals("/favicon.ico")) {
            return ResponseEntity.notFound().build();
        }

        log.error("Exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        if (path.startsWith("/api/")) {
            Map<String, Object> body = new HashMap<>();
            body.put("success", false);
            body.put("message", "Errore interno del server.");
            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }
}