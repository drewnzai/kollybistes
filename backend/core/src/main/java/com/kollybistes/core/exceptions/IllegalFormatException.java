package com.kollybistes.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class IllegalFormatException extends IllegalArgumentException{

    public IllegalFormatException(String message){
        super(message);
    }
}
