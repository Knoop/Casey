package com.knoop.casey;

import com.knoop.casey.exceptions.UnknownSocketException;
import com.knoop.casey.modules.DeviceManagerModule;
import com.knoop.casey.modules.SocketManagerModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Maurice on 26-12-2015.
 */
public class SocketManager implements SocketManagerModule {


    private Map<String, Socket> sockets = new HashMap<>();

    private int counter = 0;

    private Casey casey;

    SocketManager(Casey casey) {
        this.casey = casey;
        this.casey.yarmis.modules().createModule(this, DeviceManagerModule.class);
    }


    private synchronized String createIdentifier() {
        return "device" + counter++;
    }

    @Override
    public Socket makeSocket() {
        String identifier = this.createIdentifier();
        Socket socket = new Socket(identifier);
        this.sockets.put(identifier, socket);

        return socket;
    }

    @Override
    public List<Socket> getSockets() {
        return new ArrayList<>(this.sockets.values());
    }

    public Socket getSocket(String identifier) {

        if (this.exists(identifier))
            return (Socket) casey.yarmis.modules().getModule(identifier);
        else
            throw new UnknownSocketException(identifier);
    }

    public boolean exists(String identifier) {
        return this.sockets.containsKey(identifier);
    }


    @Override
    public boolean removeSocket(String identifier) {
        if (!this.exists(identifier))
            return false;

        this.sockets.remove(identifier);
        casey.yarmis.modules().unregister(casey.yarmis.modules().getModule(identifier));
        return true;
    }
}
