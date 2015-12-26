package com.yarmis.core.translators;

import com.yarmis.core.Communication;
import com.yarmis.core.JSONTranslator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Maurice on 22-12-2015.
 */
public class ClassTranslator implements JSONTranslator<Class> {

    @Override
    public JSONObject toJSON(Class object, Communication c) throws ClassCastException {
        try {
            return new JSONObject().put("class name", object.getName());
        } catch (JSONException e) {
            throw (ClassCastException) new ClassCastException("Cannot convert object to JSON").initCause(e);
        }
    }

    @Override
    public Class<?> fromJSON(String identifier, JSONObject o, Communication c) throws ClassCastException {
        try {
            return Class.forName(o.getString("class name"));
        } catch (ClassNotFoundException e) {
            throw (ClassCastException) new ClassCastException("Cannot convert JSON to object").initCause(e);
        }
    }
}
