package com.yarmis.core.annotations;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DemandRights {

    String[] value();

}
