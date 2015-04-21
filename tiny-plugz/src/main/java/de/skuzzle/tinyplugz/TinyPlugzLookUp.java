package de.skuzzle.tinyplugz;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.tinyplugz.TinyPlugzConfigurator.TinyPlugzImpl;

/**
 * Internal strategy interface for creating TinyPlugz instances according to
 * properties within a map.
 *
 * @author Simon Taddiken
 */
abstract class TinyPlugzLookUp {

    /**
     * Strategy for determining the TinyPlugz implementation using the
     * {@link ServiceLoader}
     */
    public static final TinyPlugzLookUp SPI_STRATEGY = new SPITinyPlugzLookup();

    /** Strategy for creating a default implementation of TinyPlugz */
    public static final TinyPlugzLookUp DEFAULT_INSTANCE_STRATEGY =
            new TinyPlugzLookUp() {

        @Override
        TinyPlugz getInstance(ClassLoader classLoader, Map<Object, Object> props)
                throws TinyPlugzException {
            return new TinyPlugzImpl();
        }
    };

    /**
     * Strategy for using a property defined class as TinyPlugz implementation.
     */
    public static final TinyPlugzLookUp STATIC_STRATEGY = new StaticTinyPlugzLookup();

    private TinyPlugzLookUp() {
        // hidden constructor
    }

    /**
     * Creates a TinyPlugz instance, configuring it according to the given
     * properties.
     *
     * @param classLoader The parent ClassLoader.
     * @param props Configuration properties.
     * @return The created instance.
     * @throws TinyPlugzException If configuring the new instance fails.
     */
    abstract TinyPlugz getInstance(ClassLoader classLoader, Map<Object, Object> props)
            throws TinyPlugzException;


    private static final class SPITinyPlugzLookup extends TinyPlugzLookUp {

        private static final Logger LOG = LoggerFactory.getLogger(TinyPlugzLookUp.class);

        @Override
        public TinyPlugz getInstance(ClassLoader classLoader, Map<Object, Object> props)
                throws TinyPlugzException {
            final Iterator<TinyPlugz> providers = ServiceLoader
                    .load(TinyPlugz.class, classLoader)
                    .iterator();

            final TinyPlugz impl = providers.hasNext()
                    ? providers.next()
                    : DEFAULT_INSTANCE_STRATEGY.getInstance(classLoader, props);

            if (providers.hasNext()) {
                LOG.warn("Multiple TinyPlugz bindings found on class path");
                providers.forEachRemaining(provider ->
                        LOG.debug("Ignoring TinyPlugz provider '{}'",
                                provider.getClass().getName()));
            }
            return impl;
        }
    }

    private static final class StaticTinyPlugzLookup extends TinyPlugzLookUp {

        @Override
        public TinyPlugz getInstance(ClassLoader classLoader, Map<Object, Object> props)
                throws TinyPlugzException {
            final String className = props.get(TinyPlugzConfigurator.FORCE_IMPLEMENTATION)
                    .toString();

            // as by precondition check in the configurator.
            assert className != null;

            try {
                final Class<?> cls = classLoader.loadClass(className);
                if (!TinyPlugz.class.isAssignableFrom(cls)) {
                    throw new TinyPlugzException(String.format(
                            "'%s' does not extend TinyPlugz", cls.getName()));
                }
                return (TinyPlugz) cls.newInstance();
            } catch (ClassNotFoundException | InstantiationException
                    | IllegalAccessException e) {
                throw new TinyPlugzException(
                        "Error while instantiating static TinyPlugz implementation", e);
            }
        }
    }
}
