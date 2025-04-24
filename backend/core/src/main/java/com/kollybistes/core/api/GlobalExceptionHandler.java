package com.kollybistes.core.api;

import com.kollybistes.core.exceptions.*;
import com.kollybistes.core.util.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


    /* HttpStatus.BAD_REQUEST 400
    HttpStatus.UNAUTHORIZED 401
    HttpStatus.NOT_FOUND 404
    HttpStatus.INTERNAL_SERVER_ERROR 500*/

    @ExceptionHandler(EntityNotFoundException.class)
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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleWrongPassword(BadCredentialsException e){
        ErrorResponse errorResponse = new ErrorResponse("Wrong password");
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({
            UsernameNotFoundException.class,
            NullPointerException.class
    })
    public ResponseEntity<ErrorResponse> handleUserNotExisting(Exception e){
        ErrorResponse errorResponse = new ErrorResponse("Username does not exist");
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({
            MalformedJwtException.class,
            ExpiredJwtException.class,
            UnsupportedJwtException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedJWT(Exception e){
        ErrorResponse errorResponse = new ErrorResponse("JWT is malformed");
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            UserNotVerifiedException.class
    })
    public ResponseEntity<ErrorResponse> handleUserNotVerified(UserNotVerifiedException e){
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedExceptions(Exception e) {
        e.printStackTrace();

        ErrorResponse errorResponse = new ErrorResponse("An unexpected error occurred." +
                " Please contact support.");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
