package org.onebusaway.api.web.interceptors;

import org.onebusaway.api.ResponseCodes;
import org.onebusaway.api.model.ResponseBean;
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
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {
    static final int NO_VERSION = -999;

    int _defaultVersion = 0;

    int _version = -999;
    public boolean isVersion(int version) {
        if (_version == NO_VERSION)
            return version == _defaultVersion;
        else
            return version == _version;
    }

    int getReturnVersion() {
        if (_version == NO_VERSION)
            return _defaultVersion;
        return _version;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static ResponseEntity<ResponseBean> handleValidationExceptions(Map<String, List<String>> fieldErrors) {
        Map<String, String> errors = new HashMap<>();
        fieldErrors.forEach((error) -> {
            String fieldName = (FieldError) error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

       return new ResponseEntity<>(new ResponseBean(getReturnVersion(), ResponseCodes.RESPONSE_INVALID_ARGUMENT,
                "validation exception", errors),HttpStatus.valueOf(ResponseCodes.RESPONSE_INVALID_ARGUMENT));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ResponseBean> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {

        return new ResponseEntity<>(new ResponseBean(getReturnVersion(), ResponseCodes.RESPONSE_INVALID_ARGUMENT,
                "type mismatch exception", Objects.requireNonNull(ex.getRootCause()).getMessage()),HttpStatus.valueOf(ResponseCodes.RESPONSE_INVALID_ARGUMENT));

    }
}