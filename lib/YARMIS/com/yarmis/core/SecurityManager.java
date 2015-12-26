package com.yarmis.core;

import com.yarmis.core.exceptions.InsufficientRightsException;
import com.yarmis.core.exceptions.NoDeclaredRightsException;
import com.yarmis.core.annotations.DemandRights;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by Maurice on 10-11-2015.
 */
public class SecurityManager extends Manager {

    /**
     * All rights that have been defined by modules that are registered
     */
    private Set<String> rights = new HashSet<>();


    /**
     * A cache of all assigned rights for devices.
     */
    private PermissionRepository permissions;

    /**
     * Creates a new Security Manager, to be used by the given YARMIS instance.
     *
     * @param yarmis
     */
    protected SecurityManager(Yarmis yarmis, PermissionRepository permissions) {
        super(yarmis);
        this.permissions = permissions;
    }

    public PermissionRepository getPermissions() {
        return permissions;
    }

    /**
     * Validates the given method call. If the given device is not allowed to make the given request, an exception will
     * be thrown. If the device is allowed to make the request, the call to this method will terminate normally.
     *
     * @throws InsufficientRightsException
     * @throws NoDeclaredRightsException
     */
    void validateMethod(Method method, Device device)
            throws NoDeclaredRightsException, InsufficientRightsException {
        List<String> rights = permissions.getPermissions(device);

        // Are the rights the device has enough to perform the call?
        this.validateMethodCall(method, rights);

    }

    /**
     * <p> Checker to see whether the given Connection is allowed to call the given Method. This will check whether the
     * rights that the method demands are a subset of all rights assigned to the connection. </p> <p> Do not that a
     * method call is also not permitted if there are no rights (null), however, that is not checked here. </p>
     *
     * @param method The method to validate
     * @param rights The rights for which to check whether the method may be invoked.
     * @throws NoDeclaredRightsException
     * @throws InsufficientRightsException
     */
    private void validateMethodCall(Method method, List<String> rights)
            throws NoDeclaredRightsException, InsufficientRightsException {

        // Get the Rights that have been demanded.
        DemandRights requiredRightAnnotation = method
                .getAnnotation(DemandRights.class);

        // If no rights have been demanded, then disallow calling to
        // prevent
        // illegal access.
        if (requiredRightAnnotation == null)
            throw new NoDeclaredRightsException(
                    "The intended method "
                            + method.getName()
                            + " is not callable. Did you forget to add @DemandRights to the method?");

        // Create a list of all missing rights
        List<String> insufficient = new LinkedList<>(Arrays.asList(requiredRightAnnotation
                .value()));
        insufficient.removeAll(rights);

        // If that list contains any elements, then it is not allowed to be
        // executed.
        if (insufficient.size() > 0)
            throw new InsufficientRightsException(insufficient);

    }

    /**
     * Called whenever a new Module is added by the ModuleManager. All rights that are required for any method of that
     * module are
     *
     * @param functionalityDefinition The class defining the functionality of the added module.
     * @param <Functionality>         The functionality class of the newly added module.
     */
    <Functionality> void onModuleAdded(Class<Functionality> functionalityDefinition) {

        for (Method method : functionalityDefinition.getMethods()) {
            DemandRights requiredRightAnnotation = method
                    .getAnnotation(DemandRights.class);
            if(requiredRightAnnotation != null)
                Collections.addAll(this.rights, requiredRightAnnotation.value());
        }

    }
}
