package com.yarmis.core;

import com.yarmis.core.exceptions.TimeoutException;
import com.yarmis.core.messages.Response;

import java.lang.reflect.InvocationTargetException;


/**
 * Result is a class that helps you to get a value from any Connection. It allows your thread to wait for the result
 * that is send over a Connection. If the result is an exception, then this exception will be thrown.
 *
 * @author Maurice
 */
public class Result {

    /**
     * The object that is the result of a request.
     */
    private Object result = null;

    /**
     * Boolean indicating whether the result has been unpacked.
     */
    private boolean isUnpacked = false;

    /**
     * <p> Indicates whether an exception should be thrown if available. The value is equal to the success attribute of
     * Response. </p>
     */
    private boolean success;

    /**
     * <p> Indicates whether this Result has already been released. If it has already been released when get() is
     * called, that thread doesn't have to wait. </p> <p> This is not entirely equal to checking whether result is null.
     * It is very well possible that the actual result is null.Use this variable instead of that check to make sure that
     * you have the correct information. </p>
     */
    private boolean hasReleased = false;

    /**
     * Create a new Result.
     */
    Result() {
    }

    /**
     * <p> Waits for the result and returns it as soon as it is available. The returned Object can be casted safely to
     * the type that you expect. </p> <p> Use this method if you can't continue working until you have a result. If you
     * however can not or don't want to block the thread waiting for the result, use addOnResultReceivedListener
     * instead. </p>
     *
     * @return
     * @throws Exception
     */
    public Object get() throws Exception {

        // wait for the result to be set but only it hasn't released before.
        synchronized (this) {
            do
                this.wait();
            while (!this.hasReleased);
        }

        // Check whether the actual value has been unpacked, if not, unpack.
        if (!this.isUnpacked)
            this.unpackResult();

        // when you get here, result has been set. Check whether it is an exception.
        if (!this.success) {
            Exception e = (Exception) this.result;
            while(InvocationTargetException.class.isAssignableFrom(e.getClass())) {
                e = (Exception) e.getCause();
            }
            throw (Exception) e;
        }

        // If no exception has been thrown, just return it.
        return this.result;

    }

    /**
     * Unpacks the result if the result hasn't yet been unpacked.
     */
    private void unpackResult() {
        if (!this.isUnpacked)
            this.result = ((Response) this.result).value();
        this.isUnpacked = true;
    }

    /**
     * Set the given response as the result. This will also release all threads waiting for this response.
     *
     * @param response
     */

    void set(Response response) {
        // Release with the given (packed) value, and throw exceptions in case of error.
        this.releaseWithValue(response, response.isSuccess(), false, false);
    }

    /**
     * Causes a timeout for this Result. While this behaves similar to {@code Result.set(e)} where e is a {@code
     * TimeoutException}, it doesn't throw an exception if the value was already set.
     * <p/>
     * This method is only intended to let all waiting threads continue if a result takes too long. If a result was
     * already set, then the threads will already have released, thus meaning that the desired effect has been
     * accomplished anyway.
     */
    void timeout() {
        this.releaseWithValue(new TimeoutException(), false, true, true);
    }

    /**
     * @param result     The result to store
     * @param success    Indicator for whether the result was obtained succesfully, or is an exception that needs
     *                   throwing instead.
     * @param isUnpacked Indicator for whether the result can be handed over directly, or whether it should be unpacked
     *                   first. The latter can only be the case if the given object is an instance of Response. The
     *                   given value for {@code isUnpacked} is not stored directly. The value of {@code
     *                   Result.isUnpacked} is only used if the given result is an instance of Response. In all other
     *                   cases it will be set to false anyway.
     * @param safe       Indicates whether this method should be executed safely. {@code safe} indicates whether an
     *                   exception should <b>not</b> be thrown in the situation that {@code result} was already set.
     */
    private synchronized void releaseWithValue(Object result, boolean success, boolean isUnpacked, boolean safe) {

        // See whether it was already released, and if so, determine whether an exception is required.
        if (this.hasReleased)
            if (!safe)
                throw new IllegalStateException(
                        "The result has already been set. It can only be set once.");
            else
                return;

        // Not released yet, release it with the given result as the obtained value.
        this.success = success;
        this.result = result;
        this.isUnpacked = isUnpacked || !(result != null && result.getClass().equals(Response.class));
        this.hasReleased = true;
        this.notify();
    }
}
