package com.kollybistes.core.exceptions;

public class WalletLockedException extends RuntimeException{
    public WalletLockedException(String message){
        super(message);
    }
}
