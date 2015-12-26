package com.yarmis.core;

import com.yarmis.core.annotations.AddsListener;
import com.yarmis.core.annotations.RemovesListener;
import com.yarmis.core.logging.Log;
import com.yarmis.core.messages.Notification;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

class ModuleInvocationHandler<RemoteClass> implements InvocationHandler,
        Module<RemoteClass> {
    /**
     * Boolean indicating whether the methods will be executed remotely.
     */
    // While ModuleInvocationHandler.implementation equals null, isRemote should
    // be true.
    private boolean isRemote = true;

    /**
     * The method cache. This maps methods that are received in the {@code invoke} method to methods in the local
     * implementation, as these methods are not equal.
     */
    private Map<Method, Method> methodCache;

    /**
     * All listeners based on their listening class
     */
    private final Map<Class<?>, Set<Object>> listeners = new HashMap<>();

    /**
     * The listener that is provided by the NotificationManager to listen to any update on the {@code Module} that this
     * {@code ModuleInvocationHandler} represents.
     */
    private Object notificationListener = null;

    /**
     * The implementation to use for local usage.
     */
    private RemoteClass implementation = null;

    /**
     * Indicates whether {@code ModuleInvocationHandler.implementationIsAnonymous} is an anonymous class. If the
     * implementing class is anonymous, then we will not have access to invoke its methods. So if this is the case, we
     * will force access to the method we're trying to invoke.
     */
    private boolean implementationIsAnonymous = false;

    /**
     * The class that defines the functionality of the Module Proxy that this is the InvocationHandler for.
     */
    private final Class<?> functionalityDefinitionClass;

    /**
     * The identifier for this Module. This is used as a handle for the Module Proxy that this is the InvocationHandler
     * for.
     */
    private final String identifier;

    /**
     * The ModuleManager that manages this module
     */
    private final ModuleManager moduleManager;

    /**
     * Flag indicating that calls to the ModuleInvocationHandler are done as part of the construction phase. This starts
     * as {@code true}, and must explicitly be set to {@code false}. This value should only once be {@code true}. Once
     * it is set to {@code false} it must remain {@code false}
     */
    private boolean isConstructing = true;

    /**
     * Creates a new ModuleInvocationHandler with the given class being the class that defines the functionality of the
     * Module Proxy that this is the InvocationHandler for. This should only be called by the ModuleManager, and only
     * when creating a new Module. Not in any other situation.
     *
     * @param functionalityDefinitionClass The class that defines the functionality of the Module Proxy that this is the
     *                                     InvocationHandler for. For instance, if you create a {@code Module} that
     *                                     behaves as if it is an instance of {@code SomeDefinition}, then the
     *                                     functionalityDefinition would be equal to {@code SomeDefinition.class}.
     * @param identifier                   The identifier for the Module Proxy that this is the InvocationHandler for.
     * @param moduleManagerr
     */
    ModuleInvocationHandler(Class<?> functionalityDefinitionClass, String identifier, ModuleManager moduleManagerr) {
        this.functionalityDefinitionClass = functionalityDefinitionClass;
        this.identifier = identifier;
        this.moduleManager = moduleManagerr;
    }


    /**
     * Called when a method is invoked on a Module proxy.
     *
     * @param object    The proxy instance the method was called on
     * @param method    The method that was called
     * @param arguments The arguments that were supplied to the method call
     * @return The result of invoking the method
     * @throws Throwable
     */
    public Object invoke(Object object, Method method, Object[] arguments) throws Throwable {
        return this.invokeAs(this.invokeType(method, arguments), method, arguments);
    }

    /**
     * Called when a method is invoked on a Module proxy.
     *
     * @param type      The InvocationType, indicating how it should be invoked.
     * @param method    The method that was called
     * @param arguments The arguments that were supplied to the method call
     * @return The result of invoking the method
     * @throws Throwable
     */
    private Object invokeAs(InvokeType type, Method method, Object... arguments)
            throws Throwable {

        switch (type) {

            case LOCAL:
                return this.invokeLocally(method, arguments);
            case REMOTE:
                return this.invokeRemotely(method, arguments);
            case ADD_LISTENER:
                return this.invokeAddListener(method, arguments);
            case REMOVE_LISTENER:
                return this.invokeRemoveListener(method, arguments);
            case OBJECT:
            case MODULE:
                return this.invokeAsModuleMethod(method, arguments);
            default:
                throw new IllegalStateException("No invocation type found for given method");
        }
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Set this up to let calls be handled remotely. This checks whether all security requirements are in check.
     */
    public void setRemote() {
        // Shortcut!
        if (this.isRemote)
            return;

        this.performSetRemote();
    }

    /**
     * Performs what is required to set this as remote. <b>DO NOT CALL THIS METHOD ANYWHERE OTHER THAN IN {@code
     * setRemote}</b>. This method requires safety/synchronization features which are verified and done by {@code
     * setRemote}.
     */
    private void performSetRemote() {
        assert (!this.isRemote());

        this.isRemote = true;
    }

    /**
     * Set this up to let calls be handled locally. This checks whether all security requirements are in check.
     */
    public void setLocal() {
        // Shortcut!
        if (!this.isRemote)
            return;

        // No implementation -> can't be local
        if (this.implementation == null)
            throw new IllegalStateException(
                    "Can't be set to local as there is no local implementation available.");

        // When everything is checked: do it.
        this.performSetLocal();

    }

    /**
     * Performs what is required to set this as local. <b>DO NOT CALL THIS METHOD ANYWHERE OTHER THAN IN {@code
     * setLocal}</b>. This method requires safety/synchronization features which are verified and done by {@code
     * setLocal}.
     */
    private void performSetLocal() {
        assert (this.isRemote());

        // Set it as a local.
        this.isRemote = false;

        // Make sure the method cache exists.
        if (methodCache == null)
            methodCache = new HashMap<>();

    }

    public void useLocalImplementation(RemoteClass localInstance) {

        if (Proxy.isProxyClass(localInstance.getClass()))
            throw new IllegalArgumentException(
                    "The provided local implementation can not be a proxy class.");

        this.onLocalImplementationChanging();
        this.implementation = localInstance;
        // Check for the class' anonymity.
        this.implementationIsAnonymous = localInstance.getClass().isAnonymousClass();
        this.onLocalImplementationChanged();
    }

    private void onLocalImplementationChanging() {
        this.callForAllListeners(RemovesListener.class);
    }

    private void onLocalImplementationChanged() {
        if (this.implementation != null && this.notificationListener == null)
            this.requestNotificationListener();

        this.callForAllListeners(AddsListener.class);
    }

    private void requestNotificationListener() {
        List<Class<?>> listeners = new LinkedList<>();
        for (Method method : this.functionalityDefinitionClass.getMethods())
            if (method.getAnnotation(AddsListener.class) != null && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].isInterface())
                listeners.add(method.getParameterTypes()[0]);

        this.notificationListener = this.moduleManager.yarmis.notifications.createListener(listeners.toArray(new Class<?>[listeners.size()]), this);
    }


    /**
     * Calls all methods that are annotated with the given annotation for all listeners for which it is applicable.
     * Methods are only called if they have the given annotation, if they have exactly one parameter and if the type of
     * that parameter is stored as a listener. Only then will the method be called locally for all listeners of that
     * type.
     *
     * @param annotation The annotation for which to call all methods.
     */
    private void callForAllListeners(Class<? extends Annotation> annotation) {

        if (this.implementation != null) {

            for (Method method : this.functionalityDefinitionClass.getMethods()) {

                // Skip every method that lacks the annotation or doesn't have exactly 1 parameter that is stored
                if (method.getAnnotation(annotation) != null && method.getParameterTypes().length == 1) {

                    // Call the method for every listener that we have stored.
                    if (this.listeners.containsKey(method.getParameterTypes()[0])) {
                        for (Object listener : this.listeners.get(method.getParameterTypes()[0])) {
                            try {
                                this.invokeAs(InvokeType.LOCAL, method, listener);
                            } catch (Throwable throwable) {
                                Log.e("MIH", throwable);
                            }
                        }
                    }

                    // And call it for the notification listener too
                    if (this.notificationListener != null) {
                        try {
                            System.out.println("Calling method " + method.getName() + " for general listener");
                            this.invokeAs(InvokeType.LOCAL, method, this.notificationListener);
                        } catch (Throwable throwable) {
                            Log.e("MIH", throwable);
                        }
                    }
                }
            }
        }
    }


    /**
     * Indicates whether all method calls will be executed remotely.
     *
     * @return
     */
    boolean isRemote() {
        return this.isRemote;
    }

    /**
     * Call to let the method be executed as if it was defined by Module. This asserts that the given Method is indeed
     * declared by the Module interface.
     * <p/>
     * The call to any method is passed onto the correct Method in this class, which implements Module.
     *
     * @param method    The method to invoke.
     * @param arguments The arguments to pass to the call of the method.
     */
    private Object invokeAsModuleMethod(Method method, Object[] arguments)
            throws Throwable {

        assert (method.getDeclaringClass().equals(Module.class));
        Log.v("MIH", "Invoking " + method.getDeclaringClass().getName() + "." + method.getName() + " as module method");
        return this.getClass()
                .getMethod(method.getName(), method.getParameterTypes())
                .invoke(this, arguments);
    }

    /**
     * Call to let the method be executed remotely.
     *
     * @param method    The method to be executed remotely.
     * @param arguments The arguments provided to the method.
     */
    private Object invokeRemotely(Method method, Object[] arguments)
            throws Throwable {

        Log.v("MIH", "Invoking " + method.getDeclaringClass().getName() + "." + method.getName() + " remotely");
        return this.moduleManager.request(this.getIdentifier(), method, arguments).get();
    }


    /**
     * Call to let the method be executed locally.
     *
     * @param method    The method to be executed locally.
     * @param arguments The arguments provided to the method.
     */
    private Object invokeLocally(Method method, Object[] arguments)
            throws Throwable {

        Log.v("MIH", "Invoking " + method.getDeclaringClass().getName() + "." + method.getName() + " locally");
        return lookupImplementingMethod(method).invoke(implementation,
                arguments);
    }

    /**
     * Calls the NotificationManager to use the invoked method to register a listener.
     *
     * @param method    The method that was called and that contained the {@code \@AddsListener} annotation. It is
     *                  assumed that the first parameter of this method indicates the type of the listener.
     * @param arguments The arguments to the method, where the first element is assumed to be the listener that should
     *                  be used.
     * @return The result of registering the given listener with the {@code NotificationManager}. If there is a local
     * implementation, then the result of invoking the method locally is returned. Otherwise {@code null} is returned.
     */
    private Object invokeAddListener(Method method, Object[] arguments) throws Throwable {

        if (arguments.length == 0)
            throw new IllegalArgumentException("At least one argument is required");

        Log.i("MIH", "Invoked a method that adds a new listener");

        // Assume the first param indicates the class
        Class<?> listenerClass = method.getParameterTypes()[0];

        Set<Object> specificListeners = this.listeners.get(listenerClass);
        boolean newListenerType = false;

        // If not yet known, create a new set and remember that it was new
        if (specificListeners == null) {
            specificListeners = new HashSet<>();
            this.listeners.put(listenerClass, specificListeners);
            newListenerType = true;
        }
        specificListeners.add(arguments[0]);


        // New type, register for NotificationManager
        if (newListenerType) {
            Log.i("MIH", "Newly added listener was first of its kind");
            this.moduleManager.yarmis.notifications.onNewListenerRegistered(this.moduleManager.getModule(this.identifier), listenerClass);
        }


        if (this.canInvokeLocally())
            return this.invokeLocally(method, arguments);
        else
            return null;

    }

    /**
     * Calls the NotificationManager to use the invoked method to unregister a listener.
     *
     * @param method    The method that was called and that contained the {@code \@AddsListener} annotation. It is
     *                  assumed that the first parameter of this method indicates the type of the listener.
     * @param arguments The arguments to the method, where the first element is assumed to be the listener that no
     *                  longer should be used.
     * @return The result of unregistering the given listener with the {@code NotificationManager}.
     */
    private Object invokeRemoveListener(Method method, Object[] arguments) throws Throwable {

        if (arguments.length == 0)
            throw new IllegalArgumentException("At least one argument is required");

        Class<?> listenerClass = method.getParameterTypes()[0];
        Set<Object> specificListeners = this.listeners.get(listenerClass);

        // If it was known, remove it, and check whether there are still listeners left
        if (specificListeners != null) {
            boolean existed = specificListeners.remove(arguments[0]);
            if (specificListeners.size() == 0) {
                this.listeners.remove(listenerClass);
                this.moduleManager.yarmis.notifications.onLastListenerUnregistered(this.moduleManager.getModule(this.identifier), listenerClass);
            }
            return existed;
        } else return false;

    }


    /**
     * <p> Looks up the Method that is the local implementation of the given method. These Methods are looked up in a
     * cache in case the Method was retrieved before. If it has not been retrieved before, then it is searched in the
     * class of attribute {@code implementation}. If the cache exists, then the found Method is stored in the cache.
     * </p> <p> If a method is cached, the Method can always be found. If it has not been cached, then a local
     * implementation of {@code RemoteClass} must be assigned to be able to find the method. </p> <p> If a cache is
     * available, but does not contain the requested {@code Method}, then the {@code Method} that was found will be
     * added to the cache. </p>
     *
     * @param method The method of which an implementation in {@code RemoteClass} must be found.
     * @return The Method as implemented in {@code RemoteClass}. This is never {@code null}.
     * @throws IllegalStateException If the Method has not been cached and there is no value set for attribute {@code
     *                               implementation}.
     * @throws NoSuchMethodException If the given Method has no implementation in {@code RemoteClass}.
     */
    private Method lookupImplementingMethod(Method method)
            throws NoSuchMethodException {

	    /* Simple case: the method was cached, return the result */
        if (this.methodCache != null && this.methodCache.containsKey(method)) {
            return this.methodCache.get(method);
        }
        /* Harder case: the method was not cached, go fetch */
        else {
        /* Local implementation is required for its class. */
            if (this.implementation == null)
                throw new IllegalStateException(
                        "Method was not cached and no local implementation was set.");

            /*
             * Lookup the method, if it doesn't exist a NoSuchMethodException is
             * thrown.
             */
            Method foundMethod = implementation.getClass().getMethod(
                    method.getName(), method.getParameterTypes());
            /*
             * Use setAccessible only when it is absolutely required.
             * For regular classes there is no problem with accessibility, only with anonymous classes.
             */
            if (this.implementationIsAnonymous)
                foundMethod.setAccessible(true);

	        /* Cache the result if a cache is present. */
            if (this.methodCache != null)
                methodCache.put(method, foundMethod);

	        /* Return the result. */
            return foundMethod;
        }

    }


    /**
     * Called by the {@code ModuleManager} when this {@code Module} is being registered.
     */
    public void onRegister() {

    }

    /**
     * Called by the {@code ModuleManager} when this {@code Module} is being unregistered. After being unregistered it
     * shouldn't be used anymore. No more requests will arrive from outside.
     */
    public void onUnregister() {

    }

    @Override
    public void notify(Notification notification) {

        for (Object listener : this.listeners.get(notification.method.getDeclaringClass())) {

            try {
                notification.method.invoke(notification.method.getDeclaringClass().cast(listener), notification.arguments);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                Log.e("MIH", "Couldn't notify listener for " + notification);
            }

        }

    }


    /**
     * @return Whether this ModuleInvocationHandler is allowed to invoke something locally
     */
    private boolean canInvokeLocally() {

        return this.implementation != null;
    }

    @Override
    public Class<?> getInterface() {
        return this.functionalityDefinitionClass;
    }

    /**
     * Indicate that the ModuleInvocationHandler must no longer be in its construction phase.
     */
    void endConstruction() {
        this.isConstructing = false;
    }

    /**
     * Determines the InvokeType for the given method and arguments. This indicates how the method should be treated
     * when invoked.
     *
     * @param method    The method for which to find the invokeType.
     * @param arguments The arguments supplied to the given method.
     * @return A value of {@code InvokeType} that indicates how the method should be treated.
     */
    private InvokeType invokeType(Method method, Object[] arguments) {

        // Method is defined by Module
        if (method.getDeclaringClass().equals(Module.class))
            return InvokeType.MODULE;

            // Method is used to add a listener
        else if (method.getAnnotation(AddsListener.class) != null)
            return InvokeType.ADD_LISTENER;

            // Method is used to remove a listener
        else if (method.getAnnotation(RemovesListener.class) != null)
            return InvokeType.REMOVE_LISTENER;

            // Current state is to execute remotely, so do that
        else if (isRemote)
            return InvokeType.REMOTE;

            // Method is to be executed locally, so do that
        else
            return InvokeType.LOCAL;

    }

    private enum InvokeType {
        LOCAL, REMOTE, ADD_LISTENER, REMOVE_LISTENER, OBJECT, MODULE
    }
}