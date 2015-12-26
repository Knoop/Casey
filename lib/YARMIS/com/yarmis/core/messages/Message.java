package com.yarmis.core.messages;

import com.yarmis.core.Communication;
import com.yarmis.core.Module;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

public abstract class Message {

    /**
     * Constructor for messages that have been received from elsewhere.
     *
     * @param object THe JSONObject that contains all data that is required to create the message.
     * @param c      The Communication instance that can be used to decode the JSONObject.
     */
    protected Message(JSONObject object, Communication c) {

    }

    /**
     * Constructor for locally build messages
     */
    protected Message() {

    }

    /**
     * Create a translation for properties
     *
     * @return
     */
    public JSONObject translate(Communication c) throws JSONException {
        return (new JSONObject()).put(Communication.TYPE, this.getClass().getSimpleName().toLowerCase());
    }


    public static Message from(JSONObject obj, Communication c) throws JSONException {
        String type = obj.getString(Communication.TYPE);

        if (type != null) {
            switch (type.toLowerCase()) {
                case Communication.NOTIFICATION:
                    return new Notification(obj, c);
                case Communication.REQUEST:
                    return new Request(obj, c);
                case Communication.RESPONSE:
                    return new Response(obj, c);
                case Communication.HOOK:
                    return new Hook(obj, c);

            }
        }
        throw new IllegalArgumentException("The given JSONObject was not a valid message");
    }

    public static Request makeRequest(String identifier, String recipient, Method m, Object[] args) {
        return new Request(identifier, recipient, m, args);
    }

    public static Response makeResponse(String identifier, Object result, boolean isSuccess) {
        return new Response(identifier, result, isSuccess);
    }

    /**
     * Creates a new Notification that indicates that the given method should be invoked on the listeners of the given
     * module that define that method.
     *
     * @param module         The module of which the listeners must be notified
     * @param listenerMethod The method to invoke on the listeners. Which listeners are to be notified is based on the
     *                       defining class of the given method.
     * @param arguments      The arguments to supply when invoking on the given listener.
     */
    public static Notification makeNotification(Module module, Method listenerMethod, Object[] arguments) {
        return new Notification(module, listenerMethod, arguments);
    }

    /**
     * Creates a new Notification that indicates that the sender of this notification has changed its interest in the
     * events that are defined by the given listener for the given module. This may mean that the sender must be added,
     * or must be removed as a listener. Which of the two it is is indicated by {@code isRegistering}.
     *
     * @param module        The module on which the interest of the sender has changed.
     * @param listenerClass The class of the listener defining the events that the sender may or may not be interested
     *                      in.
     * @param isRegistering indicates whether the sender wants to register, or wants to unregister. If set to {@code
     *                      true}, the notification indicates that the sender wants to be added as this type of
     *                      listener. When set to {@code false} it means that it wants to be removed.
     */
    public static Hook makeHook(String identifier, Module module, Class<?> listenerClass, boolean isRegistering) {
        return new Hook(identifier, module, listenerClass, isRegistering);
    }
}