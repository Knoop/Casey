package com.yarmis.core.messages;

import com.yarmis.core.Communication;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Maurice on 22-12-2015.
 */
public abstract class IdentifyableMessage extends Message {

    protected final String identifier;

    public IdentifyableMessage(String identifier) {
        this.identifier = identifier;
    }

    public IdentifyableMessage(JSONObject object, Communication communication) {
        super(object, communication);
        this.identifier = object.getString(Communication.IdentifyableMessage.IDENTIFIER);

    }

    @Override
    public JSONObject translate(Communication communication) throws JSONException {

        return super.translate(communication)
                .put(Communication.IdentifyableMessage.IDENTIFIER, this.identifier);

    }

    public String getIdentifier() {
        return identifier;
    }
}
