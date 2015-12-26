package com.knoop.casey.exceptions;

/**
 * Created by Maurice on 23-12-2015.
 */
public class UnknownSocketException extends RuntimeException {

    public UnknownSocketException(String identifier) {
        super("No socket known for identifier \"" + identifier + "\"");

    }
}
