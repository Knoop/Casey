package com.yarmis.core;

import com.yarmis.core.logging.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Maurice on 27-11-2015.
 */
public class NotificationManager extends Manager {

    private Map<Class<?>, NotificationListenersEntry> listeners = new HashMap<>();

    /**
     * @param yarmis
     */
    protected NotificationManager(Yarmis yarmis) {
        super(yarmis, "notifications");
    }

    /**
     * Create a listener that implements all given classes. This listener will redirect any call to any of its methods
     * to {@code NotificationManager.reportListenerCall}.
     *
     * @param listeners       The array of listeners that ought to be implemented.
     * @param module          The module for which the listener will be used.
     * @param <Functionality> The functionality implemented by the given module.
     * @return A listener that implements all given classes and redirects any call to any of its methods to {@code
     * NotificationManager.reportListenerCall}.
     */
    protected <Functionality> Object createListener(Class<?>[] listeners, final Module<Functionality> module) {

        // Create a proxy that can handle all interfaces above, and let it call reportListenerCall.
        return Proxy.newProxyInstance(
                module.getClass().getClassLoader(), listeners,
                new InvocationHandler() {

                    final Module<Functionality> _module = module;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {

                        // If it is a method that was defined by object, then we must invoke it on this
                        if (method.getDeclaringClass().equals(Object.class)) {
                            return this.getClass()
                                    .getMethod(method.getName(), method.getParameterTypes())
                                    .invoke(this, arguments);
                        }
                        // All other methods should be reported
                        else {
                            NotificationManager.this.reportListenerCall(_module, method, arguments);
                            return null;
                        }
                    }
                });

    }

    /**
     * Call to notify the NotificationManager that one of its listeners was called
     *
     * @param reporter The module that fired the event that let to this listener call
     * @param method   The method that was invoked on the listener
     * @param args     The arguments that were supplied to the listener
     */
    private <Functionality> void reportListenerCall(Module<Functionality> reporter, Method method, Object[] args) {

        Log.i(super.TAG, "Received listener call to method " + method.getName() + " for module "+reporter.getIdentifier());
        if (this.listeners.containsKey(method.getDeclaringClass()))
            yarmis.communication.notify(reporter, method, args, this.listeners.get(method.getDeclaringClass()).getDevices());
        else
            Log.i(super.TAG, "There are no listeners to report listener call " + method.getName() + " to");

    }

    /**
     * Register the given listeningDevice as a listening Device with the given class
     *
     * @param listenerClass   The class for which to use the listening Device
     * @param listeningDevice The listening Device that must be called when the notifications it is listening for
     *                        occur.
     * @return Whether registering the given listening Device has been successful.
     * @throws IllegalArgumentException If the given listening Device is not an instance of the provided class.
     */
    boolean register(Class<?> listenerClass, Device listeningDevice) {

        Log.i(super.TAG, "Device " + listeningDevice + " is registered as listener for " + listenerClass);

        NotificationListenersEntry entry = listeners.get(listenerClass);
        // If no entry exist, create one and store it
        if (entry == null) {
            entry = new NotificationListenersEntry(listenerClass);
            this.listeners.put(listenerClass, entry);
        }

        // The result now is the result of adding
        return entry.register(listeningDevice);
    }


    /**
     * Called to indicate that a new type of listeners was registered for the NotificationManager.
     *
     * @param listenerClass The class defining the listener
     */
    protected void onNewListenerRegistered(Module module, Class<?> listenerClass) throws Exception {
        if (this.yarmis.connection.hasHost()) {
            Log.i(super.TAG, "First listener added for " + listenerClass + " notifying host");
            this.yarmis.communication.hook(module, listenerClass, true, this.yarmis.connection.getHost()).get();
        } else {
            Log.i(super.TAG, "First listener added for " + listenerClass + " but there is no host to hook it to");
        }

    }

    /**
     * Called to indicate that a type of listeners is no longer listened to.
     *
     * @param listenerClass The class defining the listener
     */
    protected void onLastListenerUnregistered(Module module, Class<?> listenerClass) throws Exception {
        if (this.yarmis.connection.hasHost()) {
            Log.i(super.TAG, "Last listener removed for " + listenerClass + " unhooking from host");
            this.yarmis.communication.hook(module, listenerClass, false, this.yarmis.connection.getHost()).get();
        }
    }

    /**
     * Unregisters the given listener as a listening Device with the given class
     *
     * @param listenerClass   The class for which no longer to use the listener
     * @param listeningDevice The listening Device that should no longer be used.
     * @return Whether unregistering the given listener has been successful.
     * @throws IllegalArgumentException If the given listener is not an instance of the provided class.
     */

    boolean unregister(Class<?> listenerClass, Device listeningDevice) throws Exception {
        Log.i(super.TAG, "Device " + listeningDevice + " is removed as listener for " + listenerClass);

        NotificationListenersEntry entry = listeners.get(listenerClass);

        return entry != null && entry.unregister(listeningDevice);

    }

    /**
     * Drops the given class as a listener that devices are listening to. This means that all listeners that are stored
     * under this class are dropped.
     *
     * @param listenerClass The listener class that needs to be dropped.
     */
    private void drop(Class<?> listenerClass) {
        this.listeners.remove(listenerClass);
    }


    /**
     * Contains all listener instances for a given listener class. Includes functionality to drop itself from the
     * mapping of listeners if all listeners have unregistered themselves for this class.
     */
    private class NotificationListenersEntry {

        private final Set<Device> listeners = new HashSet<>();

        private final Class<?> listenerClass;

        private NotificationListenersEntry(Class<?> listenerClass) {
            this.listenerClass = listenerClass;
        }

        private boolean register(Device listeningDevice) {
            Log.i(TAG, "Registering listener of type " + this.listenerClass.getName());
            return this.listeners.add(listeningDevice);
        }

        private boolean unregister(Device listeningDevice) {
            Log.i(TAG, "Unregistering listener of type " + this.listenerClass.getName());
            boolean result = this.listeners.remove(listeningDevice);

            // If no listeners remain, remove this.
            if (result && this.listeners.size() <= 0)
                NotificationManager.this.drop(this.listenerClass);

            return result;
        }

        private Device[] getDevices() {
            return listeners.toArray(new Device[listeners.size()]);
        }


    }
}
