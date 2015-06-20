package de.skuzzle.tinyplugz.internal;

import java.util.Iterator;
import java.util.ServiceLoader;

import de.skuzzle.tinyplugz.util.ElementIterator;
import de.skuzzle.tinyplugz.util.ReflectionUtil;
import de.skuzzle.tinyplugz.util.Require;

/**
 * Internal strategy interface for abstracting loading of services.
 *
 * @author Simon Taddiken
 * @since 0.3.0
 */
public abstract class ServiceLoaderWrapper {

    private static volatile ServiceLoaderWrapper defaultImpl =
            new ServiceLoaderWrapperImpl();

    /**
     * Gets the default ServiceLoaderWrapper instance.
     *
     * @return The default instance.
     */
    public static ServiceLoaderWrapper getDefault() {
        return defaultImpl;
    }

    /**
     * Restores the default implementation.
     */
    public static void restore() {
        defaultImpl = new ServiceLoaderWrapperImpl();
    }

    /**
     * Sets the source from which ServiceLoaderWrapper instances are created by
     * {@link #getDefault()}. The given object may either be a String or
     * {@link Class} object denoting a full qualified name of a class extending
     * ServiceLoaderWrapper, or it may already be an instance of
     * ServiceLoaderWrapper.
     *
     * @param source The source to obtain a ServiceLoaderWrapper from.
     * @see ReflectionUtil#createInstance(Object, Class, ClassLoader)
     */
    public static void setSource(Object source) {
        Require.nonNull(source, "source");
        final ClassLoader cl = ServiceLoaderWrapper.class.getClassLoader();
        defaultImpl = ReflectionUtil.createInstance(source,
                ServiceLoaderWrapper.class, cl);
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
