package dev.aidevoffice.platform;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail optimisticLockConflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This record changed during your request; reload it and try again");
    }
}
