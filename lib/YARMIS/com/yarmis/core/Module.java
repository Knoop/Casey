package com.yarmis.core;


import com.yarmis.core.messages.Notification;

public interface Module<RemoteClass> {

    String getIdentifier();

    void setRemote();

    void setLocal();

    void useLocalImplementation(RemoteClass localInstance);

    void onRegister();

    void onUnregister();

    void notify(Notification notification);

    Class<?> getInterface();
}
