package com.knoop.casey;

import com.knoop.casey.modules.DeviceManagerModule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Maurice on 23-12-2015.
 */
public class DeviceManager implements DeviceManagerModule {

    private Set<Device> devices = new HashSet<>();

    private int counter = 0;

    private Casey casey;

    DeviceManager(Casey casey) {
        this.casey = casey;
        this.casey.yarmis.modules().createModule(this, DeviceManagerModule.class);
    }


    private synchronized String createIdentifier() {
        return "device" + counter++;
    }

    @Override
    public Device makeDevice() {
        String identifier = this.createIdentifier();
        Device device = new Device(identifier);
        this.devices.add(device);

        return device;
    }

    @Override
    public List<Device> getDevices() {
        return new ArrayList<>(this.devices);
    }

    public Device getDevice(String identifier) {
        return (Device) casey.yarmis.modules().getModule(identifier);
    }

    public boolean exists(String identifier) {
        return this.devices.contains(identifier);
    }


    @Override
    public boolean removeDevice(String identifier) {
        if (!this.exists(identifier))
            return false;

        this.devices.remove(identifier);
        casey.yarmis.modules().unregister(casey.yarmis.modules().getModule(identifier));
        return true;
    }
}
