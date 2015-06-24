package de.skuzzle.tinyplugz.internal;

import java.util.Iterator;
import java.util.ServiceLoader;

import de.skuzzle.tinyplugz.util.ElementIterator;

/**
 * Internal strategy interface for abstracting loading of services.
 *
 * @author Simon Taddiken
 * @since 0.3.0
 */
public abstract class ServiceLoaderWrapper {

    private static final ServiceLoaderWrapper DEFAULT =
            new ServiceLoaderWrapperImpl();

    /**
     * Gets the default ServiceLoaderWrapper instance.
     *
     * @return The default instance.
     */
    public static ServiceLoaderWrapper getDefault() {
        return DEFAULT;
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
    public abstract <T> ElementIterator<T> loadService(Class<T> providerClass,
            ClassLoader classLoader);

    private static final class ServiceLoaderWrapperImpl extends ServiceLoaderWrapper {

        @Override
        public <T> ElementIterator<T> loadService(Class<T> providerClass,
                ClassLoader classLoader) {
            return ElementIterator.wrap(
                    ServiceLoader.load(providerClass, classLoader).iterator());
        }

    }
}
