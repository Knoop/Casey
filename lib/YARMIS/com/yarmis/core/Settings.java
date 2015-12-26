package com.yarmis.core;

import com.yarmis.core.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Maurice on 8-11-2015.
 */
public class Settings {

    @Setting
    public int COMMUNICATION_PORT = 4223;

    @Setting
    public String PERMISSION_CONFIG_FILE = "permissions.config";


    private static final String DEFAULT_SOURCE = "settings.json";

    Settings() throws FileNotFoundException, IOException {
        this(new File(DEFAULT_SOURCE));
    }

    Settings(File file) throws FileNotFoundException, IOException {
        try {
            assignSettings(new JSONObject(new JSONTokener(new FileInputStream(file))));
        } catch(JSONException e) {
            throw new IOException("Error while reading JSONObject", e);
        }
    }

    Settings(JSONObject source) throws IOException {
        this.assignSettings(source);

    }

    private void assignSettings(JSONObject source) throws IOException {
        for (Field field : Settings.getSettingFields()) {
            try {
                try {
                    JSONObject value;
                    try {
                        value = source.getJSONObject(field.getName().replace('_', '.').toLowerCase());
                    } catch (JSONException e) {
                        // Object was not found. But we don't mind.
                        // This attribute will simply have its default value,
                        // which might be null.
                        continue;
                    }
                    field.set(this, Communication.parsePrimitiveValue(value));
                } catch (JSONException e) {
                    // This time we do care.
                    // The object was found, but we were not able to cast it.
                    throw new IOException("Error while reading JSONObject", e);
                }
            } catch (IllegalAccessException e) {
                // Yeah, it won't.., it is made accessible
                Log.e("Settings", e);
            }
        }

    }

    private void save() throws IOException {
        this.save(new File(DEFAULT_SOURCE));
    }

    private void save(File file) throws IOException {
        JSONObject settings = new JSONObject();
        this.writeSettings(settings);

        (new OutputStreamWriter(new FileOutputStream(file))).write(settings.toString());
    }

    private void writeSettings(JSONObject sink) throws IOException {
        for (Field field : Settings.getSettingFields()) {
            try {
                sink.put(field.getName().replace('_', '.'), Communication.convertPrimitiveValue(field.get(this)));
            } catch (IllegalAccessException e) {
                // Yeah, it won't.., it is made accessible
                Log.e("Settings", e);
            } catch (JSONException e) {
                throw new IOException("Error while writing JSONObject", e);
            }
        }
    }

    private static List<Field> getSettingFields() {
        List<Field> fields = new LinkedList<Field>();
        for (Field field : Settings.class.getFields()) {
            if (field.getAnnotation(Setting.class) == null)
                continue;
            field.setAccessible(true);
            fields.add(field);
        }
        return fields;
    }

    @Documented
    @Inherited
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Setting {

    }


}
