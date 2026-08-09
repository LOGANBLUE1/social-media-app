package com.example.app.exceptions;

import com.example.app.utils.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Extends ResponseEntityExceptionHandler so the Spring MVC exception family (unsupported
 * media type, unreadable body, wrong method, missing params, failed validation) keeps its
 * correct 4xx status instead of falling through to the Exception catch-all as a 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleBadRequest(IllegalArgumentException ex) {
        return Response.error(ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Response<Void> handleUnauthorized(UnauthorizedException ex) {
        return Response.error(ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Response<Void> handleAuthenticationFailure(AuthenticationException ex) {
        return Response.error("Invalid username or password");
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Response<Void> handleConflict(ConflictException ex) {
        return Response.error(ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Response<Void> handleNotFound(NotFoundException ex) {
        return Response.error(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<Void> handleGenericException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        return Response.error("Something went wrong");
    }

    /**
     * Every ResponseEntityExceptionHandler response funnels through here, so this is where
     * the framework's default body gets replaced with our Response envelope.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        // a client mistake is not an incident: log the cause, not a stack trace
        log.warn("Request rejected [{}]: {}", status.value(), ex.getMessage());
        return new ResponseEntity<>(Response.of(status, messageFor(ex, status), null), headers, statusCode);
    }

    private String messageFor(Exception ex, HttpStatus status) {
        if (ex instanceof MethodArgumentNotValidException validationEx) {
            String fieldErrors = validationEx.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + " " + err.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return fieldErrors.isEmpty() ? "Validation failed" : fieldErrors;
        }
        if (ex instanceof MissingServletRequestParameterException missingParam) {
            return missingParam.getParameterName() + " is required";
        }
        return switch (status) {
            case UNSUPPORTED_MEDIA_TYPE -> "Content-Type must be application/json";
            case BAD_REQUEST -> "Malformed or incomplete request";
            case METHOD_NOT_ALLOWED -> "HTTP method not supported for this endpoint";
            case NOT_ACCEPTABLE -> "Cannot produce a response in the requested format";
            default -> status.getReasonPhrase();
        };
    }
}
