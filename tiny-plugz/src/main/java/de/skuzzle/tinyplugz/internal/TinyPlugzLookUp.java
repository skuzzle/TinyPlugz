package de.skuzzle.tinyplugz.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.tinyplugz.Options;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzException;
import de.skuzzle.tinyplugz.util.ReflectionUtil;

/**
 * Internal strategy interface for creating TinyPlugz instances according to
 * properties within a map.
 *
 * @author Simon Taddiken
 */
public enum TinyPlugzLookUp {
    /**
     * Strategy for determining the TinyPlugz implementation using the
     * {@link ServiceLoader}
     */
    SPI_STRATEGY {

        @Override
        public TinyPlugz getInstance(ClassLoader classLoader,
                ServiceLoaderWrapper serviceLoader, Map<Object, Object> props) {
            final Iterator<TinyPlugz> providers = serviceLoader.loadService(
                    TinyPlugz.class, classLoader);

            final TinyPlugz impl = providers.hasNext()
                    ? providers.next()
                    : DEFAULT_INSTANCE_STRATEGY.getInstance(classLoader,
                            serviceLoader, props);

            if (providers.hasNext()) {
                final boolean fail = props.get(
                        Options.FAIL_ON_MULTIPLE_PROVIDERS) != null;
                if (fail) {
                    throw new TinyPlugzException(
                            "There are multiple TinyPlugz providers");
                }
                LOG.warn("Multiple TinyPlugz bindings found on class path");
                providers.forEachRemaining(provider ->
                        LOG.debug("Ignoring TinyPlugz provider '{}'",
                                provider.getClass().getName()));
            }
            return impl;
        }

    },
    /** Strategy for creating a default implementation of TinyPlugz */
    DEFAULT_INSTANCE_STRATEGY {

        @Override
        public TinyPlugz getInstance(ClassLoader classLoader,
                ServiceLoaderWrapper serviceLoader, Map<Object, Object> props) {
            return new DefaultTinyPlugz();
        }

    },
    /** Strategy for using a property defined class as TinyPlugz implementation. */
    STATIC_STRATEGY {

        @Override
        public TinyPlugz getInstance(ClassLoader classLoader,
                ServiceLoaderWrapper serviceLoader, Map<Object, Object> props) {
            final Object value = props.get(Options.FORCE_IMPLEMENTATION);
            return ReflectionUtil.createInstance(value, TinyPlugz.class, classLoader);
        }

    };

    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugzLookUp.class);

    /**
     * Creates a TinyPlugz instance, configuring it according to the given
     * properties.
     *
     * @param classLoader The parent ClassLoader.
     * @param props Configuration properties.
     * @return The created instance.
     * @throws TinyPlugzException If configuring the new instance fails.
     */
    public final TinyPlugz getInstance(ClassLoader classLoader,
            Map<Object, Object> props) {
        final ServiceLoaderWrapper serviceLoader = ServiceLoaderWrapper.getDefault();
        return getInstance(classLoader, serviceLoader, props);
    }

    /**
     * Creates a TinyPlugz instance, configuring it according to the given
     * properties.
     *
     * @param classLoader The parent ClassLoader.
     * @param serviceLoader A loader for searching service implementations.
     * @param props Configuration properties.
     * @return The created instance.
     * @throws TinyPlugzException If configuring the new instance fails.
     */
    abstract TinyPlugz getInstance(ClassLoader classLoader,
            ServiceLoaderWrapper serviceLoader, Map<Object, Object> props);
}
