package com.yarmis.core;

import com.yarmis.core.messages.Message;
import com.yarmis.core.translators.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


public class Communication {


    // A map from type identifiers to classes.
    private final HashMap<String, Class<?>> stringToTypeMap =
            new HashMap<>();
    private final HashMap<Class<?>, String> typeToStringMap =
            new HashMap<>();
    private final HashMap<Class<?>, JSONTranslator<?>> typeToTranslatorMap =
            new HashMap<>();
    private final HashMap<Class<?>, Class<?>> typeMapping =
            new HashMap<>();

    private final Yarmis yarmis;

    public Communication(Yarmis yarmis) {
        this.yarmis = yarmis;

        registerDataType("java.lang.Exception", Exception.class, new ExceptionTranslator());
        registerDataType("java.lang.Throwable", Throwable.class, new ThrowableTranslator());
        registerDataType("java.lang.StackTraceElement", StackTraceElement.class, new StackTraceElementTranslator());
        registerDataType("java.util.ArrayList", ArrayList.class, new ArrayListTranslator());
        registerDataType("java.lang.Class", Class.class, new ClassTranslator());
    }


    public JSONArray convertArguments(Object[] arguments, Class<?>[] argumentTypes) throws JSONException {

        if (arguments.length != argumentTypes.length)
            throw new IllegalArgumentException("Arrays should have equal length");

        JSONArray conversion = new JSONArray();
        for (int i = 0; i < arguments.length; ++i)
            conversion.put(convertValue(arguments[i], argumentTypes[i]));
        return conversion;

    }

    /**
     * Converts an object into a JSONObject. The passed Object needs to be a primitive type or a custom, registered
     * type. The resulting JSONObject has two keys {@code Communication.Value.VALUE} and {@code
     * Communication.Value.TYPE}, which contain the object's JSON representation and a string representation of the
     * object's type, respectively.
     *
     * @param value The Object to be transformed to JSON
     * @return A {@code Communication.Value} wrapping the given object.
     * @throws ClassCastException should the passed Object not castable to the declared type.
     */
    private <T> JSONObject convertValue(Object value, Class<?> type) throws ClassCastException, JSONException {
        JSONObject result = new JSONObject();
        if (!validParameter(type)) {
            // We might be able to map this type to an existing type
            Class<?> supertype = findNearestType(type);
            if (supertype == null) {
                // If we were not able to obtain a supertype, complain.
                throw new JSONSerializationException("The type " + type + " cannot be converted to JSON.");
            } else {
                // store this mapping for future reference
                typeMapping.put(type, supertype);
            }
        }
        String s = convertClassToString(type);
        switch (s) {
            case TYPE_NULL:
            case TYPE_BOOLEAN_CLASS:
            case TYPE_BOOLEAN_PRIMITIVE:
            case TYPE_CHARACTER_CLASS:
            case TYPE_CHARACTER_PRIMITIVE:
            case TYPE_DOUBLE_CLASS:
            case TYPE_DOUBLE_PRIMITIVE:
            case TYPE_FLOAT_CLASS:
            case TYPE_FLOAT_PRIMITIVE:
            case TYPE_INTEGER_CLASS:
            case TYPE_INTEGER_PRIMITIVE:
            case TYPE_LONG_CLASS:
            case TYPE_LONG_PRIMITIVE:
            case TYPE_STRING:
                return convertPrimitiveValue(value, type);
            default:
                if (typeMapping.containsKey(type)) {
                    // Replace class by the mapped class
                    type = typeMapping.get(type);
                }
                // custom type
                @SuppressWarnings("unchecked")
                JSONTranslator<T> translator = ((JSONTranslator<T>) typeToTranslatorMap.get(type));
                @SuppressWarnings("unchecked")
                T tValue = (T) value;
                result.put(Value.VALUE, translator.toJSON(tValue, this));
        }
        result.put(Value.TYPE, convertClassToString(type));
        return result;
    }

    public JSONObject convertValue(Object value) throws ClassCastException, JSONException {
        return convertValue(value, value == null ? null : value.getClass());
    }

    public static JSONObject convertPrimitiveValue(Object value) throws JSONException {
        return convertPrimitiveValue(value, value.getClass());
    }

    public static JSONObject convertPrimitiveValue(Object value, Class<?> _class) throws JSONException {
        JSONObject result = new JSONObject();
        result.put(Value.VALUE, value);
        result.put(Value.TYPE, convertPrimitiveClassToString(_class));
        return result;
    }

    public static class JSONSerializationException extends IllegalArgumentException {
        public JSONSerializationException(String message) {
            super(message);
        }
    }

    private Class<?> findNearestType(Class<?> _class) {
        Class<?> candidate = null;
        // See if there is any alias that is a supertype of _class:
        for (Class<?> parent : typeMapping.keySet()) {
            if (parent.isAssignableFrom(_class)) {
                // We can map _class to the same class that we map parent to
                Class<?> mapped = typeMapping.get(parent);
                if (candidate == null || candidate.isAssignableFrom(mapped)) {
                    candidate = mapped;
                }
            }
        }
        if (candidate != null) return candidate;

        // Otherwise we need to search through all registered types.
        for (Class<?> parent : typeToStringMap.keySet()) {
            if (parent.isAssignableFrom(_class)) {
                // We can map _class to the same class that we map parent to
                if (candidate == null || candidate.isAssignableFrom(parent)) {
                    candidate = parent;
                }
            }
        }

        // Return our best bet
        return candidate;
    }

    /**
     * Converts the given JSONArray of values to a list of only the values.
     *
     * @param jsonArray The JSONArray containing all JSONObjects that follow the Value formatting.
     * @return A list of parsed Objects, in the some order as in which they were given in the JSONArray.
     */
    public Object[] convertToArguments(JSONArray jsonArray) throws JSONException {

        Object[] arguments = new Object[jsonArray.length()];
        int i = 0;
        for (Object argument : jsonArray) {
            arguments[i++] = parseValue((JSONObject) argument);
        }
        return arguments;

    }

    /**
     * Converts the given JSONArray of values to a list of class types.
     *
     * @param jsonArray The JSONArray containing all JSONObjects that follow the Value formatting.
     * @return A list of parsed classes, in the some order as in which they were given in the JSONArray.
     */
    public Class<?>[] convertToArgumentTypes(JSONArray jsonArray) throws JSONException {

        Class<?>[] argumentTypes = new Class<?>[jsonArray.length()];
        int i = 0;
        for (Object argument : jsonArray) {
            argumentTypes[i++] = parseType((JSONObject) argument);
        }
        return argumentTypes;

    }

    public static final String NOTIFICATION = "notification";

    public static final class Notification {
        public static final String NOTIFICATION = "notification";
        public static final String EVENT = "event";
        public static final String METHOD = "method";
        public static final String MODULE = "module";
        public static final String VALUES = "values";
        public static final String LISTENER = "listener";
    }

    public static final String REQUEST = "request";

    public static final class Request {
        public static final String MODULE = "module";
        public static final String METHOD = "method";
        public static final String VALUES = "values";
    }

    public static final class IdentifyableMessage {

        public static final String IDENTIFIER = "identifier";
    }

    public static final String HOOK = "hook";

    public static final class Hook {
        public static final String UPDATE = "update";
        public static final String LISTENER = "listener";
    }

    public static final String RESPONSE = "response";

    public static final class Response {

        public static final String IDENTIFIER = "identifier";
        public static final String SUCCESS = "success";
        public static final String VALUE = "value";
    }

    public static class Value {
        public static final String TYPE = "type";
        public static final String VALUE = "value";
    }

    public static final String TYPE = "type";

    public String convertClassToString(Class<?> _class) {
        String primitiveString = convertPrimitiveClassToString(_class);
        if (primitiveString.equals(DEFAULT_STRING)) {
            if (typeToStringMap.containsKey(_class))
                return typeToStringMap.get(_class);
            else if (typeMapping.containsKey(_class))
                return typeToStringMap.get(typeMapping.get(_class));
            else
                return DEFAULT_STRING;
        } else {
            return primitiveString;
        }
    }

    public static String convertPrimitiveClassToString(Class<?> _class) {
        if (_class == null)
            return TYPE_NULL;
        else if (_class.equals(Boolean.class))
            return TYPE_BOOLEAN_CLASS;
        else if (_class.equals(boolean.class))
            return TYPE_BOOLEAN_PRIMITIVE;
        else if (_class.equals(Character.class))
            return TYPE_CHARACTER_CLASS;
        else if (_class.equals(char.class))
            return TYPE_CHARACTER_PRIMITIVE;
        else if (_class.equals(Double.class))
            return TYPE_DOUBLE_CLASS;
        else if (_class.equals(double.class))
            return TYPE_DOUBLE_PRIMITIVE;
        else if (_class.equals(Float.class))
            return TYPE_FLOAT_CLASS;
        else if (_class.equals(float.class))
            return TYPE_FLOAT_PRIMITIVE;
        else if (_class.equals(Integer.class))
            return TYPE_INTEGER_CLASS;
        else if (_class.equals(int.class))
            return TYPE_INTEGER_PRIMITIVE;
        else if (_class.equals(Long.class))
            return TYPE_LONG_CLASS;
        else if (_class.equals(long.class))
            return TYPE_LONG_PRIMITIVE;
        else if (_class.equals(String.class))
            return TYPE_STRING;
        else
            return DEFAULT_STRING;
    }

    public Class<?> convertStringToClass(String type) {
        switch (type) {
            case TYPE_BOOLEAN_CLASS:
                return Boolean.class;
            case TYPE_BOOLEAN_PRIMITIVE:
                return boolean.class;
            case TYPE_CHARACTER_CLASS:
                return Character.class;
            case TYPE_CHARACTER_PRIMITIVE:
                return char.class;
            case TYPE_DOUBLE_CLASS:
                return Double.class;
            case TYPE_DOUBLE_PRIMITIVE:
                return double.class;
            case TYPE_FLOAT_CLASS:
                return Float.class;
            case TYPE_FLOAT_PRIMITIVE:
                return float.class;
            case TYPE_INTEGER_CLASS:
                return Integer.class;
            case TYPE_INTEGER_PRIMITIVE:
                return int.class;
            case TYPE_LONG_CLASS:
                return Long.class;
            case TYPE_LONG_PRIMITIVE:
                return long.class;
            case TYPE_STRING:
                return String.class;
            case TYPE_NULL:
                return Object.class;
            default:
                // If there's a custom type registered for this identifier, return it.
                // If no custom type is defined, this will return `null`.
                return stringToTypeMap.get(type);
        }
    }

    /**
     * Parses a value from a JSONObject. It expects that the JSONObject contains the keys {@code
     * Communication.Value.VALUE} and {@code Communication.Value.TYPE}.
     *
     * @param value The JSONObject that represents the value, following the requirements stated above.
     * @return The value that was parsed from the JSONObject.
     */
    public Object parseValue(JSONObject value) throws JSONException {
        String type = value.getString(Communication.Value.TYPE);
        switch (type) {
            case TYPE_NULL:
            case TYPE_BOOLEAN_CLASS:
            case TYPE_BOOLEAN_PRIMITIVE:
            case TYPE_CHARACTER_CLASS:
            case TYPE_CHARACTER_PRIMITIVE:
            case TYPE_DOUBLE_CLASS:
            case TYPE_DOUBLE_PRIMITIVE:
            case TYPE_FLOAT_CLASS:
            case TYPE_FLOAT_PRIMITIVE:
            case TYPE_INTEGER_CLASS:
            case TYPE_INTEGER_PRIMITIVE:
            case TYPE_LONG_CLASS:
            case TYPE_LONG_PRIMITIVE:
            case TYPE_STRING:
                return parsePrimitiveValue(value);
            default:
                Class<?> _class = stringToTypeMap.get(type);
                if (_class != null) {
                    // then the type was registered:
                    JSONTranslator translator = typeToTranslatorMap.get(_class);
                    return translator.fromJSON(type, value.getJSONObject(Communication.Value.VALUE), this);
                } else {
                    // the type was not registered, we don't know how to handle this
                    throw new IllegalArgumentException("There is no translator registered for values of type " + type);
                }
        }

    }

    public static Object parsePrimitiveValue(JSONObject value) throws JSONException {
        String type = value.getString(Communication.Value.TYPE);
        switch (type) {
            case TYPE_BOOLEAN_CLASS:
            case TYPE_BOOLEAN_PRIMITIVE:
                return value.getBoolean(Communication.Value.VALUE);
            case TYPE_CHARACTER_CLASS:
            case TYPE_CHARACTER_PRIMITIVE:
                return (char) value.getInt(Communication.Value.VALUE);
            case TYPE_DOUBLE_CLASS:
            case TYPE_DOUBLE_PRIMITIVE:
                return value.getDouble(Communication.Value.VALUE);
            case TYPE_FLOAT_CLASS:
            case TYPE_FLOAT_PRIMITIVE:
                return (float) value.getDouble(Communication.Value.VALUE);
            case TYPE_INTEGER_CLASS:
            case TYPE_INTEGER_PRIMITIVE:
                return value.getInt(Communication.Value.VALUE);
            case TYPE_LONG_CLASS:
            case TYPE_LONG_PRIMITIVE:
                return value.getLong(Communication.Value.VALUE);
            case TYPE_NULL:
                return null;
            case TYPE_STRING:
                return value.getString(Communication.Value.VALUE);
            default:
                throw new IllegalArgumentException("Type " + type + " is not a primitive type.");
        }
    }

    public Class<?> parseType(JSONObject jsonObject) throws JSONException {
        return convertStringToClass(jsonObject.getString(Value.TYPE));
    }

    /**
     * Creates an identifier. This consists of the IP address, followed by an @ and a time stamp. This makes it unique
     * across devices and across time.
     *
     * @return A unique identifier that can be used to identify messages across different devices.
     */
    public String getIdentifier() {
        return yarmis.connection.localAddress() + "@"
                + System.currentTimeMillis();
    }

    /**
     * Indicates whether the given class is a valid parameter. This is checked by comparing the result of converting
     * that class to a character to the default return value for that conversion.
     *
     * @param _class The class to verify as a parameter.
     * @return {@code true} if it can be used as a parameter, {@code false} otherwise.
     */
    public boolean validParameter(Class<?> _class) {

        if (DEFAULT_STRING.equals(convertClassToString(_class))) {
            Class<?> supertype = findNearestType(_class);
            if (supertype == null) {
                return false;
            } else {
                // store this mapping for future reference
                typeMapping.put(_class, supertype);
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * If messages contain custom (that is, non-primitive) data types, then Communication needs to know how to handle
     * them. All custom data types must be serializable to JSON and deserializable from JSON.
     * <p/>
     * This function is used to declare - which class should be registered, - what identifier should be used to
     * represent the class within a JSONObject, and - how to translate between an instance of the custom type and JSON.
     * The `JSONTranslator` interface is used to declare how to translate the custom type.
     * <p/>
     * It is possible to re-define what a specific identifier represents. After calling `registerDataType("typeA",
     * A.class, translatorA)` and `registerDataType("typeA", B.class, translatorB)`, the String "typeA" will represent
     * instances of class B. When a String identifier has been used previously to represent a different class as in the
     * example above, then that old class will be returned.
     *
     * @param identifier The string that is used in the JSONObject to represent this custom type. This identifier has to
     *                   be unique; otherwise it is impossible to get a bijection between objects and their JSON
     *                   representation. An `IllegalArgumentException` is thrown if either - the identifier is `null` or
     *                   empty (a whitespace-only identifier is not allowed) - the identifier is shorter than 4
     *                   characters. Short identifiers are reserved for internal usage.
     * @param _class     The class of the custom type. An `IllegalArgumentException` is thrown if there is already a
     *                   different identifier that maps to the same class.
     * @param translator The JSONTranslator instance that is used to translate between JSON and instances of _class.
     * @return if the identifier has been used previously to represent a different custom type, then that old custom
     * type's class is returned. `null` otherwise.
     * @throws IllegalArgumentException as described above
     */
    public <T> Class<?> registerDataType(String identifier, Class<T> _class, JSONTranslator<T> translator) throws IllegalArgumentException {
        if (identifier == null || identifier.trim().length() == 0) {
            throw new IllegalArgumentException("A data type identifier must not be null or empty.");
        } else if (identifier.trim().length() <= 3) {
            throw new IllegalArgumentException("Identifiers of up to 3 characters are reserved to internal data types.");
        } else if (typeToStringMap.containsKey(_class)) {
            throw new IllegalArgumentException("The identifier " + typeToStringMap.get(_class) + " is already defined for the passed class.");
        } else {
            typeToStringMap.put(_class, identifier);
            typeToTranslatorMap.put(_class, translator);
            // Check if any of the mapped classes can be made more efficient
            Set<Class<?>> keySet = typeMapping.keySet();
            for (Class<?> c : keySet) {
                if (_class.equals(c)) {
                    // We do not need to map this type any longer
                    keySet.remove(c);
                } else if (_class.isAssignableFrom(c)) {
                    // This new translator might be a better match for c
                    if (typeMapping.get(c).isAssignableFrom(_class)) {
                        // _class is a better mapping for c
                        typeMapping.put(c, _class);
                    }
                }
            }
            return stringToTypeMap.put(identifier, _class);
        }
    }

    /**
     * Reverts `registerDataType` for the given class. Functionally, this method is identical to the variant that
     * accepts a String identifier.
     *
     * @param _class The class which should not be used as a custom type any longer.
     * @return The identifier that was used to represent that class, or `null` if the given class was not registered.
     */
    public String deregisterDataType(Class<?> _class) {
        if (!typeToStringMap.containsKey(_class)) {
            return null;
        } else {
            String identifier = typeToStringMap.remove(_class);
            typeToTranslatorMap.remove(_class);
            stringToTypeMap.remove(identifier);
            removeMappedClasses(_class);
            return identifier;
        }
    }

    /**
     * Reverts `registerDataType` for the given identifier. Functionally, this method is identical to the variant that
     * accepts a class.
     *
     * @param identifier The String that identifies the class that should no longer be usable as a custom type.
     * @return The class that the given String was mapped to, or `null` if that identifier was never mapped to any
     * class.
     */
    public Class<?> deregisterDataType(String identifier) {
        if (!stringToTypeMap.containsKey(identifier)) {
            return null;
        } else {
            Class<?> _class = stringToTypeMap.remove(identifier);
            typeToTranslatorMap.remove(_class);
            typeToStringMap.remove(_class);
            removeMappedClasses(_class);
            return _class;
        }
    }

    private void removeMappedClasses(Class<?> _class) {
        // Remove any mapped classes that used the removed class
        Set<Class<?>> keySet = typeMapping.keySet();
        ArrayList<Class<?>> removed = new ArrayList<Class<?>>();
        for (Class<?> c : keySet) {
            if (_class.equals(typeMapping.get(c))) {
                keySet.remove(c);
                removed.add(c);
            }
        }
        // Try to find a new supertype for the removed classes
        for (Class<?> c : removed) {
            Class<?> supertype = findNearestType(c);
            if (supertype != null) {
                // store this mapping for future reference
                typeMapping.put(c, supertype);
            }
        }
    }


    /**
     * Creates a request to execute the given method m.
     * <p/>
     * This assumes that the given method m is valid for remote invocation. See {@code ModuleManager.createModule} for
     * the definition of when a method is valid.
     *
     * @param identifier The identifier to use for this request
     * @param m          The method that needs to be requested
     * @param args       The arguments that need to be passed to the call of the given method.
     * @return The created Request, containing a request for the invocation of the given method, with the given
     * arguments.
     */
    protected com.yarmis.core.messages.Request makeRequest(String identifier, String recipient, Method m,
                                                           Object... args) {
        return Message.makeRequest(identifier, recipient, m, args);

    }

    /**
     * Creates a JSON response for the request with the same identifier.
     *
     * @param identifier The identifier of this message.
     * @param value      The resulting value of the execution. This is either the return value (or null if void), or an
     *                   exception.
     * @param isSuccess  Indicates whether the execution was a success. The execution of a request is a success if it
     *                   terminated normally, and thus no exceptions were thrown.
     * @return The created JSONObject, containing a response for the execution of the request with the same identifier.
     */
    protected com.yarmis.core.messages.Response makeResponse(String identifier, Object value, boolean isSuccess) {
        return Message.makeResponse(identifier, value, isSuccess);
    }

    /**
     * Creates a Notification that contains all required information to let any receivers know that the given method was
     * called with the given arguments.
     *
     * @param listenerMethod
     * @param args
     * @return The created notification
     */
    protected com.yarmis.core.messages.Notification makeNotification(Module module, Method listenerMethod, Object[] args) {
        return Message.makeNotification(module, listenerMethod, args);
    }

    /**
     * Creates a Notification that contains all required information to let any receivers know that the given method was
     * called with the given arguments.
     *
     * @param listenerClass The class for which to register this device.
     * @param register      Whether the listener class should be used to register ({@code true}) or to unregister
     *                      ({@code false}).
     * @return The created notification
     */
    protected com.yarmis.core.messages.Hook makeHook(String identifier, Module module, Class<?> listenerClass, boolean register) {
        return Message.makeHook(identifier, module, listenerClass, register);
    }


    /**
     * The default String to use in the conversion from a class to a representing String.
     */
    private static final String DEFAULT_STRING = "?";

    /**
     * The String to use to express a Boolean.
     */
    private static final String TYPE_BOOLEAN_CLASS = "B";

    /**
     * The String to use to express a Boolean.
     */
    private static final String TYPE_BOOLEAN_PRIMITIVE = "b";

    /**
     * The String to use to express a Character.
     */
    private static final String TYPE_CHARACTER_CLASS = "C";

    /**
     * The String to use to express a Character.
     */
    private static final String TYPE_CHARACTER_PRIMITIVE = "c";

    /**
     * The String to use to express a Double.
     */
    private static final String TYPE_DOUBLE_CLASS = "D";

    /**
     * The String to use to express a Double.
     */
    private static final String TYPE_DOUBLE_PRIMITIVE = "d";

    /**
     * The String to use to express a Float.
     */
    private static final String TYPE_FLOAT_CLASS = "F";

    /**
     * The String to use to express a Float.
     */
    private static final String TYPE_FLOAT_PRIMITIVE = "f";

    /**
     * The String to use to express an Integer.
     */
    private static final String TYPE_INTEGER_CLASS = "I";

    /**
     * The String to use to express an int.
     */
    private static final String TYPE_INTEGER_PRIMITIVE = "i";

    /**
     * The String to use to express a Long.
     */
    private static final String TYPE_LONG_CLASS = "L";

    /**
     * The String to use to express a Long.
     */
    private static final String TYPE_LONG_PRIMITIVE = "l";
    /**
     * The String to use to express a null value.
     */
    private static final String TYPE_NULL = "n";

    /**
     * The String to use to express a String.
     */
    private static final String TYPE_STRING = "s";

    public static final class CommunicationException extends RuntimeException {

        /**
         *
         */
        private static final long serialVersionUID = -5863516554277940503L;

        public CommunicationException(String arg0, Throwable arg1,
                                      boolean arg2, boolean arg3) {
            super(arg0, arg1, arg2, arg3);
            // TODO Auto-generated constructor stub
        }

        public CommunicationException(String arg0, Throwable arg1) {
            super(arg0, arg1);
            // TODO Auto-generated constructor stub
        }

        public CommunicationException(Throwable arg0) {
            super(arg0);
            // TODO Auto-generated constructor stub
        }

    }

}