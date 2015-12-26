package com.knoop.casey.modules;

import com.knoop.casey.Device;
import com.yarmis.core.annotations.DemandRights;

import java.util.List;

/**
 * Created by Maurice on 23-12-2015.
 */
public interface DeviceManagerModule {

    @DemandRights("register_devices")
    Device makeDevice();

    @DemandRights("see_devices")
    List<Device> getDevices();

    @DemandRights("see_devices")
    boolean exists(String identifier);

    @DemandRights("remove_devices")
    boolean removeDevice(String identifier);
}
