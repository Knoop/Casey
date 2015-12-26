package com.yarmis.core.messages;

import com.yarmis.core.Communication;
import com.yarmis.core.Module;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * Created by Maurice on 17-10-2015.
 */
public class Notification extends Message {

    /**
     * The Module that this notification is about
     */
    public final String moduleIdentifier;

    /**
     * The method that must be invoked. The declaring class of this method is the listener class that must be used.
     */
    public final Method method;

    /**
     * The arguments that must be used when invoking the method associated with this notification.
     */
    public final Object[] arguments;


    /**
     * Creates a Notification from a JSONObject.
     *
     * @param jsonObject
     * @param communication
     * @throws JSONException
     */
    protected Notification(JSONObject jsonObject, Communication communication) throws JSONException {
        super(jsonObject, communication);
        this.moduleIdentifier = jsonObject.optString(Communication.Notification.MODULE, null);

        Class<?>[] parameterTypes = communication.convertToArgumentTypes(jsonObject.getJSONArray(Communication.Notification.VALUES));
        try {
            this.method = ((Class<?>) communication.parseValue(jsonObject.getJSONObject(Communication.Notification.LISTENER)))
                    .getMethod(jsonObject.getString(Communication.Notification.METHOD), parameterTypes);
        } catch (NoSuchMethodException e) {
            throw (ClassCastException) new ClassCastException("Cannot convert object to JSON").initCause(e);
        }

        this.arguments = communication.convertToArguments(jsonObject.getJSONArray(Communication.Notification.VALUES));


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
    protected Notification(Module module, Method listenerMethod, Object[] arguments) {
        this.moduleIdentifier = module != null ? module.getIdentifier() : null;
        this.method = listenerMethod;
        this.arguments = arguments;
    }

    @Override
    public JSONObject translate(Communication c) {
        return super.translate(c)
                .put(Communication.Notification.MODULE, this.moduleIdentifier)
                .put(Communication.Notification.METHOD, this.method.getName())
                .put(Communication.Notification.LISTENER, c.convertValue(this.method.getDeclaringClass()))
                .put(Communication.Notification.VALUES, c.convertArguments(this.arguments, this.method.getParameterTypes()));
    }

    public String getModuleIdentifier() {
        return moduleIdentifier;
    }


    @Override
    public String toString() {
        return "Hook for module: " + this.moduleIdentifier + " method: " + this.method.getName() + " listener: " + this.method.getDeclaringClass().getName();
    }
}
