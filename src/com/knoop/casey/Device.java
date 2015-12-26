package com.knoop.casey;

/**
 * Created by Maurice on 26-12-2015.
 */
public class Device {

    protected final String identifier;

    /**
     * The connectable item that connects this Device to the power grid.
     */
    protected Connectable connectable;

    Device(String identifier) {
        this.identifier = identifier;
    }
}
