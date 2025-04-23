package com.kollybistes.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class WalletLockedException extends RuntimeException{
    public WalletLockedException(String message){
        super(message);
    }
}
