package com.yarmis.core.translators;

import com.yarmis.core.JSONTranslator;
import com.yarmis.core.Communication;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class StackTraceElementTranslator implements JSONTranslator<StackTraceElement> {
    private static final String DECLARING_CLASS = "declaringClass";
    private static final String METHOD_NAME = "methodName";
    private static final String FILE_NAME = "fileName";
    private static final String LINE_NUMBER = "lineNumber";

    @Override
    public JSONObject toJSON(StackTraceElement e, Communication communication) throws ClassCastException {
        try {
            JSONObject result = new JSONObject();

            result.put(DECLARING_CLASS, e.getClassName());
            result.put(METHOD_NAME, e.getMethodName());
            result.put(FILE_NAME, e.getFileName());
            result.put(LINE_NUMBER, e.getLineNumber());
            
            return result;
        } catch(JSONException ee) {
            throw (ClassCastException) new ClassCastException("Cannot convert argument to JSON.").initCause(ee);
        }
    }

    @Override
    public StackTraceElement fromJSON(String identifier, JSONObject o, Communication communication) throws ClassCastException {
        try {
            return new StackTraceElement(
                    o.getString(DECLARING_CLASS), //DECLARING_CLASS
                    o.getString(METHOD_NAME),     //METHOD_NAME
                    o.optString(FILE_NAME, "Unknown source"),       //FILE_NAME
                    o.optInt(LINE_NUMBER, -1)           //LINE_NUMBER
                );
        } catch(JSONException e) {
            throw (ClassCastException) new ClassCastException("Cannot convert argument to StackTraceElement.").initCause(e);
        }
        
    }
}