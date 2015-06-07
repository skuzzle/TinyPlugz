package de.skuzzle.tinyplugz.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.tinyplugz.Options;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzException;

/**
 * Internal strategy interface for creating TinyPlugz instances according to
 * properties within a map.
 *
 * @author Simon Taddiken
 */
public abstract class TinyPlugzLookUp {

    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugzLookUp.class);

    /**
     * Strategy for determining the TinyPlugz implementation using the
     * {@link ServiceLoader}
     */
    public static final TinyPlugzLookUp SPI_STRATEGY = new SPITinyPlugzLookup();

    /** Strategy for creating a default implementation of TinyPlugz */
    public static final TinyPlugzLookUp DEFAULT_INSTANCE_STRATEGY =
            new TinyPlugzLookUp() {
                @Override
                public TinyPlugz getInstance(ClassLoader classLoader,
                        Map<Object, Object> props) {
                    return new DefaultTinyPlugz();
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
    public abstract TinyPlugz getInstance(ClassLoader classLoader,
            Map<Object, Object> props);

    private static final class SPITinyPlugzLookup extends TinyPlugzLookUp {

        @Override
        public TinyPlugz getInstance(ClassLoader classLoader, Map<Object, Object> props) {
            final Iterator<TinyPlugz> providers = ServiceLoader
                    .load(TinyPlugz.class, classLoader)
                    .iterator();

            final TinyPlugz impl = providers.hasNext()
                    ? providers.next()
                    : DEFAULT_INSTANCE_STRATEGY.getInstance(classLoader, props);

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
    }

    private static final class StaticTinyPlugzLookup extends TinyPlugzLookUp {

        @Override
        public TinyPlugz getInstance(ClassLoader classLoader, Map<Object, Object> props) {
            final Object value = props.get(Options.FORCE_IMPLEMENTATION);
            if (value instanceof TinyPlugz) {
                return (TinyPlugz) value;
            } else if (value instanceof Class<?>) {
                return fromClass((Class<?>) value);
            } else if (value instanceof String) {
                return fromClassName(classLoader, value.toString());
            } else {
                throw new TinyPlugzException(String.format(
                        "Illegal value for 'Options.FORCE_IMPLEMENTATION': %s", value));
            }
        }

        private TinyPlugz fromClass(Class<?> cls) {
            try {
                if (!TinyPlugz.class.isAssignableFrom(cls)) {
                    throw new TinyPlugzException(String.format(
                            "'%s' does not extend TinyPlugz", cls.getName()));
                }
                return (TinyPlugz) cls.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new TinyPlugzException(
                        "Error while instantiating static TinyPlugz implementation", e);
            }
        }

        private TinyPlugz fromClassName(ClassLoader classLoader, String className) {
            try {
                final Class<?> cls = classLoader.loadClass(className);
                return fromClass(cls);
            } catch (ClassNotFoundException e) {
                throw new TinyPlugzException(String.format(
                        "Class '%s' not found using '%s'", className, classLoader), e);
            }
        }
    }
}
