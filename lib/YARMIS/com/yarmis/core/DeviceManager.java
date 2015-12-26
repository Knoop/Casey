package com.yarmis.core;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The DeviceManager will be used for creating devices, keeping track of already registered devices for easy reconnects
 * and for handling the rights that may be assigned to any device.
 */
public class DeviceManager extends Manager {



    /**
     * Create a new DeviceManager
     *
     * @param yarmis The YARMIS instance for which this manager manages devices.
     */
    DeviceManager(Yarmis yarmis) {
        super(yarmis);
    }


    /**
     * Create a device under the given name and with the given address.
     *
     * @param name    The name to give to this device
     * @param address The address where the device can be found. This can be an ip address or a domainname.
     * @return The created Device
     * @throws UnknownHostException If the address couldn't be resolved.
     */
    public static Device createDevice(String name, String address) throws UnknownHostException {
        return DeviceManager.createDevice(name, InetAddress.getByName(address));
    }

    /**
     * Create a device under the given name and with the given address.
     *
     * @param name    The name to give to this device
     * @param address The address where the device can be found.
     * @return The created Device
     */
    public static Device createDevice(String name, InetAddress address) {
        return new Device(address, name);
    }

    /**
     * Create a device with the given address.
     *
     * @param address The address where the device can be found.
     * @return The created Device
     */
    public static Device createDevice(InetAddress address) {
        return new Device(address, "test");
    }

}
