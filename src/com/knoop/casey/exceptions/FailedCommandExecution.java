package com.knoop.casey.exceptions;

/**
 * Created by Maurice on 26-12-2015.
 */
public class FailedCommandExecution extends RuntimeException {
    public FailedCommandExecution() {
        super("Failed to execute command");

    }

    public FailedCommandExecution(int result) {
        super("Executed command resulted in unexpected value " + result);

    }
}
