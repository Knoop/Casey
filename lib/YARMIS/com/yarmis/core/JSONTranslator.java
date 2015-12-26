package com.yarmis.core;

import org.json.JSONObject;

public interface JSONTranslator<T> {
	public JSONObject toJSON(T object, Communication c) throws ClassCastException;
	public T fromJSON(String identifier, JSONObject o, Communication c) throws ClassCastException;

}