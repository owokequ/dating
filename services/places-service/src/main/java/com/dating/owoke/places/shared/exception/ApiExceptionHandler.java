package com.dating.owoke.places.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dating.owoke.places.place.exception.DuplicatePlaceException;
import com.dating.owoke.places.place.exception.PlaceNotFoundException;
import com.dating.owoke.places.sync.exception.SyncUnavailableException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PlaceNotFoundException.class)
    ProblemDetail notFound(PlaceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    @ExceptionHandler({DuplicatePlaceException.class, SyncUnavailableException.class})
    ProblemDetail conflict(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, "Operation cannot be completed", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ProblemDetail badRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail databaseConflict(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "Operation cannot be completed", "Place conflicts with existing data");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        return problem;
    }
}
