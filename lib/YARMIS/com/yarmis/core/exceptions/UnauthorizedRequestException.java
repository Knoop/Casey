package com.yarmis.core.exceptions;

import java.util.List;

public class UnauthorizedRequestException extends Exception {

    /**
     * A list of missing Rights.
     */
    private final List<String> missing;

    /**
     * Creates a new UnauthorizedRequestException with the given list of Rights indicating the Rights that were missing
     * to be able to make the Request.
     *
     * @param missing The rights that were missing to have been able to perform the request.
     */
    public UnauthorizedRequestException(List<String> missing) {
        super();
        this.missing = missing;
    }

    /**
     * Creates a new UnauthorizedRequestException indicating that no
     */
    public UnauthorizedRequestException() {
        this(null);
    }

    /**
     * Get an Array of all Rights that were missing, causing this UnauthorizedRequestException to be thrown.
     *
     * @return An array containing all rights that were also required to perform the request, or null if the request was
     * not allowed at all.
     */
    public String[] getMissingRights() {

        if (this.wasCallable())
            return this.missing.toArray(new String[missing.size()]);
        else
            return null;
    }

    /**
     * Indicates whether there are any Rights that would allow you to make the Request that bounced. If false, then no
     * set of Rights exist that would allow you to make the Request this was thrown for.
     *
     * @return true if a set of Rights existed to be allowed to perform the Request, false if the Request would never be
     * allowed.
     */
    public boolean wasCallable() {
        return this.missing == null;
    }

}
