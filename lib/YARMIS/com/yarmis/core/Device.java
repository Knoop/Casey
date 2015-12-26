package com.yarmis.core;

import org.json.JSONObject;
import org.json.JSONException;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Device {
    public final InetAddress address;
    public final String name;

    private static final String NAME = "name";
    private static final String ADDRESS = "address";
    
    public Device(InetAddress address, String name) {
        this.address = address;
        this.name = name;
    }

    public String toString() {
        return this.name + "@" + this.address.toString();
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject o = new JSONObject();
        o.put(NAME, name);
        o.put(ADDRESS, address.getHostAddress());
        return o;
    }

    @Override
    public int hashCode() {
        return 31 * address.hashCode() + name.hashCode();
    }

    public static Device fromJSON(JSONObject o) throws JSONException {
        String name = o.getString(NAME);
        try {
            InetAddress a = InetAddress.getByName(o.getString(ADDRESS));
            return new Device(a, name);
        } catch(UnknownHostException e) {
            throw new JSONException("Hostname has to be an IP address", e);
        }
    }
}