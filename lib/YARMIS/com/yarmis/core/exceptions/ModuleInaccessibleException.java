package com.yarmis.core.exceptions;

import com.yarmis.core.Module;

public class ModuleInaccessibleException extends RuntimeException {

    public ModuleInaccessibleException(Module module) {
        super("Module " + module.getIdentifier() + " is not accessible for remote invocation");
        // TODO Auto-generated constructor stub
    }


}
