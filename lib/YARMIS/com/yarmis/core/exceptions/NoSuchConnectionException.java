package com.yarmis.core.exceptions;

import com.yarmis.core.Device;

/**
 * Indicates that a Request has not been found.
 *
 */
public class NoSuchConnectionException extends IllegalStateException {

    public NoSuchConnectionException(Device device) {
        super("No Connection exists for device "+device);
    }
}
