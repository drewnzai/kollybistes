package com.kollybistes.core.api;

import com.kollybistes.core.exceptions.*;
import com.kollybistes.core.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler()
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException e) {
        ErrorResponse errorResponse= new ErrorResponse(e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            ExpiredTokenException.class,
            IllegalFormatException.class,
            InsufficientBalanceException.class,
            ResourceAlreadyExistsException.class,
            TransactionException.class,
            WalletLockedException.class
    })
    public ResponseEntity<ErrorResponse> handleKnownExceptions(Exception e) {
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedExceptions(Exception e) {
        e.printStackTrace(); // Prints the full stack trace to the console for debugging

        ErrorResponse errorResponse = new ErrorResponse("An unexpected error occurred." +
                " Please contact support.");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
