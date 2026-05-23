package com.operations.auth.api;

import com.operations.auth.api.dto.ErrorResponse;
import com.operations.auth.service.AuthException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ErrorResponse> handleAuth(AuthException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponse("AUTH_ERROR", ex.getMessage(), Instant.now()));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  public ResponseEntity<ErrorResponse> handleValidation(Exception ex) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse("VALIDATION_ERROR", "Invalid request payload", Instant.now()));
  }
}
