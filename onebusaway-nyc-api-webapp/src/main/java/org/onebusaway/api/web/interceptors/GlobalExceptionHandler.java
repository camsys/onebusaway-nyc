package org.onebusaway.api.web.interceptors;

import org.onebusaway.api.web.actions.api.where.FieldErrorSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, List<String>>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        FieldErrorSupport fieldErrors = new FieldErrorSupport();
//        todo: switch to this clearer error handling
//        Map<String, String> errors = new HashMap<>();
//        String fieldName = ex.getName();
//        String errorMessage = String.format("The value '%s' for field '%s' is invalid. Valid fields must be a %s.", ex.getValue(), fieldName, ex.getRequiredType().getSimpleName());
//        errors.put(fieldName, errorMessage);
        return ResponseEntity.badRequest().body(fieldErrors.invalidValue(ex.getName()).getErrors());
    }
}