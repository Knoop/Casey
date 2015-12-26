package com.yarmis.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Maurice on 27-11-2015.
 *
 * This annotation indicates that the method it targets is used to remove a listener.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RemovesListener {
}
