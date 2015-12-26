package com.yarmis.core.messages;

import com.yarmis.core.Communication;
import com.yarmis.core.Module;
import com.yarmis.core.logging.Log;
import org.json.JSONObject;

/**
 * Created by Maurice on 22-12-2015.
 */
public class Hook extends IdentifyableMessage {

    /**
     *
     */
    private final String moduleIdentifier;

    /**
     * The class that the sending device wants to listen to, or wants to stop listening to.
     */
    public final Class<?> listenerClass;


    /**
     * Indicates whether the listener should be added ({@code true}) or removed({@code false}).
     */
    public final boolean adding;


    protected Hook(String identifier, Module module, Class<?> listenerClass, boolean adding) {
        super(identifier);
        // If a module was given, get its identifier, otherwise use null
        this.moduleIdentifier = module != null ? module.getIdentifier() : null;
        this.listenerClass = listenerClass;
        this.adding = adding;
        Log.i("Hook", "listenerClass: " + listenerClass);
    }

    protected Hook(JSONObject object, Communication c) {
        super(object, c);
        this.moduleIdentifier = object.optString(Communication.Notification.MODULE, null);
        this.listenerClass = (Class<?>) c.parseValue(object.getJSONObject(Communication.Hook.LISTENER));
        this.adding = object.getString(Communication.Hook.UPDATE).equals("register");
        Log.i("Hook", "listenerClass: " + listenerClass);
    }


    @Override
    public JSONObject translate(Communication communication) {
        return super.translate(communication)
                .put(Communication.Notification.MODULE, this.moduleIdentifier)
                .put(Communication.Hook.LISTENER, communication.convertValue(this.listenerClass))
                .put(Communication.Hook.UPDATE, (adding ? "" : "un") + "register");
    }


    @Override
    public String toString(){
        return "Hook ("+this.identifier+")  for module: "+this.moduleIdentifier+" listener: "+this.listenerClass.getName()+" adding: "+this.adding;
    }
}
