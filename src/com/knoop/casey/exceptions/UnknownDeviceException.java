package com.knoop.casey.exceptions;

/**
 * Created by Maurice on 23-12-2015.
 */
public class UnknownDeviceException extends RuntimeException {

    public UnknownDeviceException(String identifier) {
        super("No device known for identifier \"" + identifier + "\"");

    }
}
