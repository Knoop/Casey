package com.yarmis.core.messages;

import com.yarmis.core.Communication;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Maurice on 17-10-2015.
 */
public class Response extends Message {

    /**
     * The identifier of the request for which this is a response.
     */
    private final String identifier;

    /**
     * Indication whether this result is a success, or whether it is an exception, or other indication of a failure
     * during the execution of the request for which this is a response.
     */
    private final boolean isSuccess;

    /**
     * The value for the response. This must be unpacked before it can be used as the actual result. This is only an
     * immediate state.
     */
    private Object value;

    private Class<?> type;

    protected Response(String identifier, Object value, boolean isSuccess) {
        this(identifier, value, value == null ? null : value.getClass(), isSuccess);
    }

    protected Response(String identifier, Object value, Class<?> type, boolean isSuccess) {
        super();
        this.identifier = identifier;
        this.value = value;
        this.type = type;
        this.isSuccess = isSuccess;
    }

    protected Response(JSONObject obj, Communication communication) throws JSONException {
        super(obj, communication);
        this.identifier = obj.getString(Communication.Response.IDENTIFIER);
        this.value = communication.parseValue(obj.getJSONObject(Communication.Response.VALUE));
        this.type = communication.parseType(obj.getJSONObject(Communication.Response.VALUE));
        this.isSuccess = obj.getBoolean(Communication.Response.SUCCESS);
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    /**
     * Unpacks the result and returns the actual value.
     *
     * @return The unpacked value.
     */
    public Object value() {
        return this.value;
    }

    @Override
    public JSONObject translate(Communication communication) throws JSONException {

        return super.translate(communication)
                .put(Communication.Response.IDENTIFIER, this.identifier)
                .put(Communication.Response.SUCCESS, this.isSuccess)
                .put(Communication.Response.VALUE, communication.convertValue(this.value));

    }

    @Override
    public String toString() {
        return "Response for identifier: " + this.identifier + " success: " + this.isSuccess;
    }
}
