package it.sensorplatform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Intercetta gli errori di JSON malformato
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonErrors(HttpMessageNotReadableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "Payload non valido o formattazione errata.");
        // Nota: omettiamo volontariamente il timestamp e il path per non dare info
        // sull'infrastruttura

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Intercetta qualsiasi altro errore imprevisto (il vero salvavita per A10:2025)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericErrors(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "Errore interno del server.");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
