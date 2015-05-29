package de.skuzzle.tinyplugz.guice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used to execute a function in the scope of the TinyPlugz Classloader as
 * current thread'scontext Classloader.
 *
 * @author Simon Taddiken
 * @since 0.2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TinyPlugzContext {

}
