package com.yarmis.core.translators;

import java.util.ArrayList;

import com.yarmis.core.JSONTranslator;
import com.yarmis.core.Communication;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;


public class ArrayListTranslator implements JSONTranslator<ArrayList> {
        
    @Override
    public JSONObject toJSON(ArrayList object, Communication c) throws ClassCastException {
        try {
            JSONObject result = new JSONObject();
            ArrayList<JSONObject> jsonCollection = new ArrayList<JSONObject>();
            for(Object o: object) {
                jsonCollection.add(c.convertValue(o));
            }
            JSONArray array = new JSONArray(jsonCollection);
            
            result.put("values", array);
            return result;
        } catch(JSONException e) {
            throw (ClassCastException) new ClassCastException("Cannot convert object to JSON").initCause(e);
        }
    }

    @Override
    public ArrayList fromJSON(String identifier, JSONObject o, Communication c) {
        try {
            JSONArray array = o.getJSONArray("values");
            ArrayList result = new ArrayList();
            for (int i = 0; i < array.length(); i++) {
                result.add(c.parseValue(array.getJSONObject(i)));
            }
            return result;
        } catch (JSONException e) {
            throw (ClassCastException) new ClassCastException("Cannot convert JSON to object").initCause(e);
        }
    }
}
