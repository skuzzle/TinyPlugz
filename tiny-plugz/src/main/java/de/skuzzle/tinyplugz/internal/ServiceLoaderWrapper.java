package de.skuzzle.tinyplugz.internal;

import java.util.Iterator;

import de.skuzzle.tinyplugz.util.ElementIterator;

/**
 * Internal strategy interface for abstracting loading of services.
 *
 * @author Simon Taddiken
 */
interface ServiceLoaderWrapper {

    /**
     * Gets a new instance of the default implementation.
     *
     * @return A new ServiceLoaderWrapper.
     */
    public static ServiceLoaderWrapper getDefault() {
        return new DefaultServiceLoaderWrapper();
    }

    /**
     * Returns an {@link Iterator} of all implementors of the given service
     * provider interface.
     *
     * @param <T> Type of the service provider interface.
     * @param providerClass The service provider interface.
     * @param classLoader The Classloader to use.
     * @return An iterator of implementors.
     */
    <T> ElementIterator<T> loadService(Class<T> providerClass, ClassLoader classLoader);
}
