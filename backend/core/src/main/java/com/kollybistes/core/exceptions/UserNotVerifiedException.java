package com.kollybistes.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class UserNotVerifiedException extends RuntimeException{

    public UserNotVerifiedException(String message){
        super(message);
    }
}
