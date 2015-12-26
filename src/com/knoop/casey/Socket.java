package com.knoop.casey;

import com.yarmis.core.logging.Log;

/**
 * Created by Maurice on 26-12-2015.
 */
public class Socket extends Connectable {

    protected final String identifier;
    protected String channel;
    protected String address;
    protected Protocol protocol;
    protected State state;

    protected Socket(String identifier) {
        this.identifier = identifier;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        Log.v(this.identifier, "Setting address " + address);
        this.address = address;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        Log.v(this.identifier, "Setting channel " + channel);
        this.channel = channel;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setState(State state) {

        Log.v(this.identifier, "Setting state " + state);
        Driver.runCommand(this.makeCommand(state));
        this.state = state;

    }

    private Driver.Command makeCommand(State state) {
        return new Driver.Command(this, state);
    }


    public enum State {
        ON, OFF
    }

    public enum Protocol {
        ACTION, BLOKKER, ELRO, KAKU
    }
}
