package com.nodoraiz.androidhooker.models;

public class HookerException extends Exception {

    private Exception innerException;
    private String hookerExceptionMessage;

    public Exception getInnerException() {
        return innerException;
    }

    public String getHookerExceptionMessage() {
        return hookerExceptionMessage;
    }

    public HookerException(Exception innerException, String hookerExceptionMessage) {
        this.innerException = innerException;
        this.hookerExceptionMessage = hookerExceptionMessage;
    }
}
