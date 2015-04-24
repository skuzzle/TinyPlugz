package de.skuzzle.tinyplugz;

import java.util.Iterator;

/**
 * Internal strategy interface for abstracting loading of services.
 *
 * @author Simon Taddiken
 */
interface ServiceLoaderWrapper {

    /**
     * Returns an {@link Iterator} of all implementors of the given service
     * provider interface.
     *
     * @param <T> Type of the service provider interface.
     * @param providerClass The service provider interface.
     * @param classLoader The Classloader to use.
     * @return An iterator of implementors.
     */
    <T> Iterator<T> loadService(Class<T> providerClass, ClassLoader classLoader);
}
