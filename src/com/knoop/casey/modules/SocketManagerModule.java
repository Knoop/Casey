package com.knoop.casey.modules;

import com.knoop.casey.Socket;
import com.yarmis.core.annotations.DemandRights;

import java.util.List;

/**
 * Created by Maurice on 26-12-2015.
 */
public interface SocketManagerModule {
    @DemandRights("register_sockets")
    Socket makeSocket();

    @DemandRights("see_sockets")
    List<Socket> getSockets();

    @DemandRights("see_sockets")
    boolean exists(String identifier);

    @DemandRights("remove_sockets")
    boolean removeSocket(String identifier);

}
