package com.dating.owoke.events.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dating.owoke.events.event.exception.EventNotFoundException;
import com.dating.owoke.events.sync.exception.SyncAlreadyRunningException;
import com.dating.owoke.events.sync.exception.SyncUnavailableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(EventNotFoundException.class)
    ProblemDetail notFound(EventNotFoundException exception) { return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage()); }
    @ExceptionHandler({SyncAlreadyRunningException.class, IllegalStateException.class})
    ProblemDetail conflict(RuntimeException exception) { return problem(HttpStatus.CONFLICT, "Operation cannot be completed", exception.getMessage()); }
    @ExceptionHandler(SyncUnavailableException.class)
    ProblemDetail unavailable(SyncUnavailableException exception) { return problem(HttpStatus.SERVICE_UNAVAILABLE, "External synchronization unavailable", exception.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ProblemDetail badRequest(Exception exception) { return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage()); }
    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail result = ProblemDetail.forStatus(status); result.setTitle(title); result.setDetail(detail); return result;
    }
}
