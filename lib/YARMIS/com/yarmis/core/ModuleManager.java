package com.yarmis.core;

import com.yarmis.core.annotations.DemandRights;
import com.yarmis.core.exceptions.ModuleInaccessibleException;
import com.yarmis.core.exceptions.UnauthorizedRequestException;
import com.yarmis.core.messages.Request;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class ModuleManager extends Manager {

    ModuleManager(Yarmis yarmis) {
        super(yarmis);
    }


    /**
     * A Set of all modules that have been made accessible for remote invocation
     */
    private Set<Module> accessibility = new HashSet<>();

    /**
     * All modules that are registered.
     */
    private Map<String, Module<?>> modules = new WeakHashMap<>();

    /**
     * Creates a new Module based on the given class. The resulting Module provides the functionality as defined by
     * {@code functionalityDefinition}. This functionality can be executed on a remote location or on the local machine.
     * To allow for the latter an implementation of the {@code functionalityDefinition} interface needs to be provided.
     *
     * @param functionalityDefinition <p> The interface that defines the functionality. An interface can be used to
     *                                create a Module if the following requirements are met: <ul> <li>It is an interface
     *                                that extends the ModularFunctionality interface</li> <li>It contains public
     *                                methods that have the {@code @DemandRights} annotation</li> </ul> </p>
     * @return A Module version of the the functionality defined by {@code functionalityDefinition}.
     */
    @SuppressWarnings("unchecked")
    public <Functionality> Functionality createModule(
            Class<Functionality> functionalityDefinition) {
        return this.createModule(functionalityDefinition, functionalityDefinition.getSimpleName());
    }

    /**
     * Creates a new Module based on the given class. The resulting Module provides the functionality as defined by
     * {@code functionalityDefinition}. This functionality can be executed on a remote location or on the local machine.
     * To allow for the latter an implementation of the {@code functionalityDefinition} interface needs to be provided.
     *
     * @param functionalityDefinition <p> The interface that defines the functionality. An interface can be used to
     *                                create a Module if the following requirements are met: <ul> <li>It is an interface
     *                                that extends the ModularFunctionality interface</li> <li>It contains public
     *                                methods that have the {@code @DemandRights} annotation</li> </ul> </p>
     * @param identifier              The identifier to use for the module. Ensure that the naming is consistent across
     *                                devices.
     * @return A Module version of the the functionality defined by {@code functionalityDefinition}.
     */
    @SuppressWarnings("unchecked")
    public <Functionality> Functionality createModule(
            Class<Functionality> functionalityDefinition, String identifier) {

        // validate the given class
        validateClass(functionalityDefinition);

        // We know it is valid, so create a handler
        ModuleInvocationHandler<Functionality> mih =
                new ModuleInvocationHandler<>(functionalityDefinition, identifier, this);

        // Use the handler to create a new Module Proxy.
        Module<Functionality> module = (Module<Functionality>) Proxy
                .newProxyInstance(
                        functionalityDefinition.getClassLoader(),
                        ModuleManager.requiredModuleInterfaces(functionalityDefinition)
                        , mih);


        // Register the rights
        yarmis.security.onModuleAdded(functionalityDefinition);

        // Register the Module
        this.register(module);

        // Specify that we are no longer constructing it.
        mih.endConstruction();

        return (Functionality) module;

    }

    /**
     * Determine which classes a proxy must implement to be a Module for the given functionality. This result can be
     * plugged directly into the proxy creation.
     *
     * @param functionalityDefinition The class that represents the intended functionality
     * @return An array of all required interfaces. Currently this contains only {@code functionalityDefinition} and
     * {@code Module.class}
     */
    public static Class<?>[] requiredModuleInterfaces(Class<?> functionalityDefinition) {
        return new Class<?>[]{functionalityDefinition, Module.class};
    }

    /**
     * Creates a new module for the given functionality, using the instance as a local implementation. This Module is
     * then also set to be used as local.
     *
     * @param <Functionality>         The class for which the Module needs to be defined
     * @param instance                The Functionality instance to use as the local implementation
     * @param functionalityDefinition The Funcitonality class for which to define the Module
     * @return A Module that implements the Functionality class and that uses the given instance to handle local calls.
     * This Module is set to use its local implementation until setRemote is called.
     */
    public <Functionality> Functionality createModule(Functionality instance, Class<? extends Functionality> functionalityDefinition, String identifier) {
        Functionality created = this.createModule(functionalityDefinition, identifier);
        ((Module<Functionality>) created).useLocalImplementation(instance);
        ((Module<Functionality>) created).setLocal();
        return created;
    }

    /**
     * Creates a new module for the given functionality, using the instance as a local implementation. This Module is
     * then also set to be used as local.
     *
     * @param <Functionality>         The class for which the Module needs to be defined
     * @param instance                The Functionality instance to use as the local implementation
     * @param functionalityDefinition The Funcitonality class for which to define the Module
     * @return A Module that implements the Functionality class and that uses the given instance to handle local calls.
     * This Module is set to use its local implementation until setRemote is called.
     */
    public <Functionality> Functionality createModule(Functionality instance, Class<? extends Functionality> functionalityDefinition) {
        Functionality created = this.createModule(functionalityDefinition);
        ((Module<Functionality>) created).useLocalImplementation(instance);
        ((Module<Functionality>) created).setLocal();
        return created;
    }


    /**
     * Register the given {@code Module}. This will unregister a {@code Module} with the same identifier, unless that
     * {@code Module} is actually the same as this {@code Module}. This will make the {@code Module} eligible for remote
     * invocation. This does not require the {@code Module} to actually be invoked from remote locations.
     *
     * @param module The {@code Module} to register.
     */
    public void register(Module module) {

        Module current = this.modules.get(module.getIdentifier());

        if (current != null) {
            // Equal? No use
            if (current.equals(module))
                return;
            // Unequal? Unregister the old if required.
            this.unregister(current);
        }

        // Register the new
        this.modules.put(module.getIdentifier(), module);

        // Let it know it is registered
        module.onRegister();
    }

    /**
     * Unregister the given {@code Module}. This will stop the {@code Module} from being eligible for remote
     * invocation.
     *
     * @param module The {@code Module} to unregister.
     */
    public void unregister(Module module) {

        // Let it know it will be unregistered
        module.onUnregister();

        // Unregister the old
        this.modules.remove(module.getIdentifier());
    }

    public Module<?> getModule(String identifier) {
        return this.modules.get(identifier);
    }


    /**
     * <p> Validates that the given class is suited for being used as a Module. If the given class is not suited, then a
     * describing exception will be thrown. If it is suited, the call will terminate without exceptions. </p>
     * <h5>Suitability</h5> <p> A class is suitable for remote execution if all of the following requirements are met:
     * <ul> <li>It is an interface that extends the ModularFunctionality interface</li> <li>It contains public methods
     * that have the {@code @DemandRights} annotation</li> </ul> </p>
     *
     * @param _class <p> The class for which to find out whether it is suited for usage as a Module. </p>
     * @return True if the class matches the requirements above, false otherwise.
     */
    private void validateClass(Class<?> _class) {

        // Check whether methods exist that can be called if it is used as a
        // Module.
        boolean hasAccessibleMethods = false;

        for (Method method : _class.getMethods())
            hasAccessibleMethods |= validateMethod(method);

        // If there are no accessible methods, then it is not allowed.
        if (!hasAccessibleMethods)
            throw new NoAccessibleMethodsException(_class);

    }

    /**
     * Validates the given {@code Method}. A {@code Method} is valid for remote invocation if it meets the following
     * requirements: <ul> <li>It is public</li> <li>It has the {@code @DemandRights} annotation</li> <li>Its parameters
     * are valid</li> </ul>
     *
     * @param method The {@code Method} to verify.
     * @return
     */
    private boolean validateMethod(Method method) {
        if (Modifier.isPublic(method.getModifiers())
                && method.getAnnotation(DemandRights.class) != null) {
            for (Class<?> _class : method.getParameterTypes())
                if (!yarmis.communication.communication.validParameter(_class))
                    return false;
            return true;

        } else
            return false;

    }

    /**
     * Handles the given request. It performs security checks to see whether the request can be executed if it
     * originates from the given connection.
     *
     * @param request    The request to handle
     * @param connection The connection from which the request originated
     * @return The result of executing the request
     * @throws Throwable In case an exception occurs during the execution of the request. Either due to security reasons
     *                   or due to the fact that the execution itself failed.
     */
    Object handleRequest(Request request, Connection connection)
            throws Throwable {

        String recipient = request.getRecipient();

        /*
         * MODULE VALIDATION
         *
         * Check whether the module has been cleared for remote invocation by the connection.
         * throws exception if not
         */

        Module module = this.getModule(recipient);
        if (module == null)
            throw new ModuleInaccessibleException(module);
        this.validateModule(module, connection.getDevice());

        /*
         * METHOD VALIDATION
         *
         * Check whether the method has been cleared for remote invocation by the connection.
         * throws exception if not
         */
        Method method = module.getInterface().getMethod(request.getMethod(), request.getArgumentTypes());

        if (method == null)
            throw new UnauthorizedRequestException();

        yarmis.security.validateMethod(method, connection.getDevice());

        // It is allowed.
        return method.invoke(this.modules.get(recipient), request.getArguments());

    }

    /**
     * Validates the given module. If the given module is not accessible for remote invocation, an exception will be
     * thrown. If the module is accessible, the call to this method will terminate normally.
     *
     * @param module
     * @throws ModuleInaccessibleException
     */
    private void validateModule(Module module, Device device)
            throws ModuleInaccessibleException {
        // Is the intended module accessible?
        if (!this.isAccessible(module))
            throw new ModuleInaccessibleException(module);
    }

    /**
     * Makes the given module accessible for executing code remotely.
     *
     * @param module The Module to make accessible
     */
    public void makeAccessible(Module module) {
        this.accessibility.add(module);
    }

    /**
     * Makes the given module inaccessible for executing code remotely. When the given module receives a Request, an
     * exception will be thrown.
     *
     * @param module The module to make inaccessible
     */
    public void makeInaccessible(Module module) {
        this.accessibility.remove(module);
    }

    /**
     * Makes all modules accessible for executing code remotely
     */
    public void makeAllAccessible() {
        this.accessibility.addAll(modules.values());
    }

    /**
     * Makes all modules inaccessible for executing code remotely. When any module receives a Request, an exception will
     * be thrown.
     */
    public void makeAllInaccessible() {
        this.accessibility.clear();
    }

    /**
     * Indicates whether the module for the given name is accessible for remote invocation.
     *
     * @param module The module for which to check accessibility
     * @return true if the module can be used for remote invocation, false otherwise.
     */
    private boolean isAccessible(Module module) {
        return this.accessibility.contains(module);
    }

    /**
     * Requests for the given method to be executed by the Host.
     *
     * @param method    The Method to execute on the Host.
     * @param arguments The arguments to provide to the Host
     * @return A Result object that can be used to retrieve the return value.
     */
    public Result request(String identifier, Method method, Object[] arguments) {
        return this.yarmis.communication.request(identifier, method, arguments);
    }

    /**
     * <p> Exception that indicates that the class mentioned in the exception does not contain any methods that can be
     * accessed when used remotely. </p> <p> A method is available for remote execution if all of the following
     * requirements are met: <ul> <li>The method is publicly available;</li> <li>The method has the {@code
     *
     * @author Maurice
     * @DemandRights} annotation (the demanded {@code Rights} are not important);</li> </ul> </p>
     */
    public static final class NoAccessibleMethodsException extends
            RuntimeException {

        /**
         * Generated serial version universal identifier.
         */
        private static final long serialVersionUID = -2805803932827960855L;

        // Only the ModuleManager may create a new instance of this exception.
        private NoAccessibleMethodsException(Class<?> _class) {
            super(
                    _class.getName()
                            + " doesn't have any accessible methods for remote execution.");
        }

    }

    public static final class IllegalAccessibleMethodException extends
            RuntimeException {

        /**
         *
         */
        private static final long serialVersionUID = 1277651592679680465L;

        // Only the ModuleManager may create a new instance of this exception.
        private IllegalAccessibleMethodException(Method method) {
            super(
                    method.toString()
                            + " is set available for remote execution but has invalid parameters.");
        }

    }

    public static final class NotDeclaredAsModularFunctionalityException extends
            RuntimeException {

        /**
         * Generated serial version universal identifier.
         */
        private static final long serialVersionUID = -176165158865175565L;

        // Only the ModuleManager may create a new instance of this exception.
        private NotDeclaredAsModularFunctionalityException(Class<?> _class) {
            super(_class.getName()
                    + " doesn't extend the ModularFunctionality interface.");
        }

    }

}
