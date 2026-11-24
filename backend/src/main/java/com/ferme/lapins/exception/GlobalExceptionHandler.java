package com.ferme.lapins.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<Map<String, String>> rep(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(Map.of("message", m));
    }
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) { return rep(HttpStatus.NOT_FOUND, e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> bad(IllegalArgumentException e) { return rep(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) { return rep(HttpStatus.BAD_REQUEST, "Données invalides : vérifiez l’email et le mot de passe (6 caractères minimum)"); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException e) { return rep(HttpStatus.CONFLICT, "Opération impossible : données incohérentes"); }
}
