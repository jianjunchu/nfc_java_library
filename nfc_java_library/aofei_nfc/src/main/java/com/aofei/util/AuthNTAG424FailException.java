package com.aofei.util;

public class AuthNTAG424FailException extends Exception{

    private String hexCode;

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public AuthNTAG424FailException(String hexCode,String message) {
        super(message);
        this.hexCode = hexCode;
    }

    public String getHexCode() {
        return hexCode;
    }
}
