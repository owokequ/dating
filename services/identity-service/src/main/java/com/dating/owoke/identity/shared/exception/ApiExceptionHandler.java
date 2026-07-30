package com.dating.owoke.identity.shared.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dating.owoke.identity.account.exception.AccountNotFoundException;
import com.dating.owoke.identity.authentication.exception.AuthenticationRejectedException;
import com.dating.owoke.identity.authentication.exception.InvalidAccountTokenException;
import com.dating.owoke.identity.telegram.exception.TelegramOidcException;
import com.dating.owoke.identity.telegram.exception.TelegramOidcUnavailableException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationRejectedException.class)
    ProblemDetail authenticationRejected(AuthenticationRejectedException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidAccountTokenException.class)
    ProblemDetail invalidToken(InvalidAccountTokenException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(TelegramOidcException.class)
    ProblemDetail telegramRejected(TelegramOidcException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(TelegramOidcUnavailableException.class)
    ProblemDetail telegramUnavailable(TelegramOidcUnavailableException exception, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail notFound(AccountNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ProblemDetail badRequest(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
    }

    private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
