package com.yarmis.core.messages;

import com.yarmis.core.Communication;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * Created by Maurice on 17-10-2015.
 */
public class Request extends IdentifyableMessage {

    /**
     * The intended recipient for this request
     */
    private final String recipient;

    /**
     * The name of the intended method to be executed
     */
    private final String method;

    /**
     * The types of the arguments of the intended method to be executed. Before this can be accessed, {@code unpack()}
     * must be called.
     */
    private Class<?>[] argumentTypes;

    /**
     * The actual arguments of the intended method to be executed. Before this can be accessed, {@code unpack()} must be
     * called.
     */
    private Object[] arguments;

    protected Request(String identifier, String recipient, Method m, Object[] arguments) {
        super(identifier);
        this.recipient = recipient;
        this.method = m.getName();
        this.argumentTypes = m.getParameterTypes();
        this.arguments = arguments != null ? arguments : new Object[0];
    }

    protected Request(JSONObject obj, Communication communication) throws JSONException {
        super(obj, communication);
        this.recipient = obj.getString(Communication.Request.MODULE);
        this.method = obj.getString(Communication.Request.METHOD);
        this.argumentTypes = communication.convertToArgumentTypes(obj.getJSONArray(Communication.Request.VALUES));
        this.arguments = communication.convertToArguments(obj.getJSONArray(Communication.Request.VALUES));
    }


    /**
     * Get the identifier of the Request.
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Get the name of the intended recipient.
     *
     * @return The name of the intended recipient.
     */
    public String getRecipient() {
        return this.recipient;
    }

    /**
     * Get the name of the intended method
     *
     * @return
     */
    public String getMethod() {
        return method;
    }

    /**
     * Get the argument types of the request
     *
     * @return A class array containing all classes of the arguments that need to be supplied for the intended method.
     * These classes are in order.
     */
    public Class<?>[] getArgumentTypes() {
        return this.argumentTypes;
    }

    /**
     * Get the arguments of the request
     *
     * @return An Object array containing all classes of the arguments that need to be supplied for the intended method.
     * These Objects are unpacked to the correct classes and are placed in the required order.
     */
    public Object[] getArguments() {

        return this.arguments;
    }


    @Override
    public JSONObject translate(Communication communication) throws JSONException {

        return super.translate(communication)
                .put(Communication.Request.MODULE, this.recipient)
                .put(Communication.Request.METHOD, this.method)
                .put(Communication.Request.VALUES, communication.convertArguments(this.arguments, this.argumentTypes));

    }

    @Override
    public String toString() {
        return "Request ("+this.identifier+") for module: " + this.recipient + " method: " + this.method;
    }


}
