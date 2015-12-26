package com.yarmis.core.translators;

import com.yarmis.core.JSONTranslator;
import com.yarmis.core.Communication;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class ExceptionTranslator implements JSONTranslator<Exception> {
    private static final String MESSAGE = "message";
    private static final String CAUSE = "cause";
    private static final String CLASS = "class";
    private static final String STACKTRACE = "stacktrace";

    @Override
    public JSONObject toJSON(Exception e, Communication communication) throws ClassCastException {
        try {
            JSONObject result = new JSONObject();

            if(e.getMessage() != null) {
                result.put(MESSAGE, e.getMessage());
            }

            if(e.getCause() != null) {
                result.put(CAUSE, communication.convertValue(e.getCause()));
            }

            result.put(CLASS, e.getClass().getName());

            StackTraceElement[] stackTrace = e.getStackTrace();
            result.put(STACKTRACE, new JSONArray());
            for (StackTraceElement s : stackTrace) {
                result.append(STACKTRACE, communication.convertValue(s));
            }

            return result;
        } catch(JSONException ee) {
            throw (ClassCastException) new ClassCastException("Cannot convert object to JSON").initCause(ee);
        }
    }

    @Override
    public Exception fromJSON(String identifier, JSONObject o, Communication communication) throws ClassCastException {
        try {
            try {
                String message = null;
                Throwable cause = null;
                StackTraceElement[] stackTrace = new StackTraceElement[0];
                
                if(o.has(MESSAGE)) {
                    message = o.getString(MESSAGE);
                }

                if(o.has(CAUSE)) {
                    JSONObject causeObject = o.getJSONObject(CAUSE);
                    cause = (Throwable) communication.parseValue(causeObject);
                }

                if(o.has(STACKTRACE)) {
                    JSONArray traceArray = o.getJSONArray(STACKTRACE);
                    int stackLength = traceArray.length();
                    stackTrace = new StackTraceElement[stackLength];
                    for (int i = 0; i < stackLength; i++) {
                        stackTrace[i] = (StackTraceElement) communication.parseValue(traceArray.getJSONObject(i));
                    }
                }

                Exception e;
                if(message != null && cause != null) {
                    e = (Exception)
                        Class.forName(o.getString(CLASS))
                        .getConstructor(String.class, Throwable.class)
                        .newInstance(message, cause);
                } else if(message != null) {
                    e = (Exception)
                        Class.forName(o.getString(CLASS))
                        .getConstructor(String.class)
                        .newInstance(message);
                } else if(cause != null) {
                    e = (Exception)
                        Class.forName(o.getString(CLASS))
                        .getConstructor(Throwable.class)
                        .newInstance(cause);
                } else {
                    e = (Exception)
                        Class.forName(o.getString(CLASS))
                        .getConstructor()
                        .newInstance();
                }
                e.setStackTrace(stackTrace);
                return e;
            } catch(Exception ee) {
                throw (ClassCastException) new ClassCastException("Cannot convert argument of type " + o.getString(CLASS) + " to Exception.").initCause(ee);
            }
        } catch(JSONException ee) {
            throw (ClassCastException) new ClassCastException("Cannot convert argument to Exception.").initCause(ee);
        }
    }
}