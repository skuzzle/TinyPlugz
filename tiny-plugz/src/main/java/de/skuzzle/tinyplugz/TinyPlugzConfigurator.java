package de.skuzzle.tinyplugz;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a fluent builder API for configuring an application wide single
 * {@link TinyPlugz} instance.
 *
 * @author Simon Taddiken
 */
public final class TinyPlugzConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugz.class);
    private static final Object INIT_LOCK = new Object();

    private TinyPlugzConfigurator() {}

    /**
     * Sets up a {@link TinyPlugz} instance which uses the current thread's
     * context Classloader as parent Classloader.
     *
     * <p>
     * This method will fail immediately if TinyPlugz already has been
     * configured.
     * </p>
     *
     * @return Fluent builder object for further configuration.
     */
    public static DefineProperties setup() {
        return new Impl(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Sets up a {@link TinyPlugz} instance which uses the given Classloader as
     * parent Classloader.
     * <p>
     * This method will fail immediately if TinyPlugz already has been
     * configured.
     * </p>
     *
     * @param parentClassLoader The parent Classloader to use.
     * @return Fluent builder object for further configuration.
     */
    public static DefineProperties setupUsingParent(ClassLoader parentClassLoader) {
        return new Impl(parentClassLoader);
    }

    /**
     * Sets up a {@link TinyPlugz} instance which uses the Classloader which
     * loaded the {@link TinyPlugzConfigurator} class as parent Classloader.
     * <p>
     * This method will fail immediately if TinyPlugz already has been
     * configured.
     * </p>
     *
     * @return Fluent builder object for further configuration.
     */
    public static DefineProperties setupUsingApplicationClassLoader() {
        return new Impl(TinyPlugzConfigurator.class.getClassLoader());
    }

    public static interface DefineProperties {

        /**
         * Specifies a single property to insert into the map which will be
         * passed to
         * {@link TinyPlugz#initializeInstance(java.util.Set, ClassLoader, Map)}
         * .
         *
         * @param name Name of the property.
         * @param value Value of the property.
         * @return A fluent builder object for further configuration.
         */
        DefineProperties withProperty(String name, Object value);

        /**
         * Specifies a multiple properties to insert into the map which will be
         * passed to
         * {@link TinyPlugz#initializeInstance(java.util.Set, ClassLoader, Map)}
         * .
         *
         * @param values Mappings to add.
         * @return A fluent builder object for further configuration.
         */
        DefineProperties withProperties(Map<String, ? extends Object> values);

        /**
         * Provides the {@link PluginSource} via the given consumer for adding
         * plugins which should be deployed.
         *
         * @param source
         * @return A fluent builder object for further configuration.
         */
        DeployTinyPlugz withPlugins(Consumer<PluginSource> source);
    }

    public interface DeployTinyPlugz {

        /**
         * Finally deploys the {@link TinyPlugz} instance using the the
         * configured values. The configured instance will be globally
         * accessible using {@link TinyPlugz#getDefault()}.
         *
         * @return The configured instance.
         */
        public TinyPlugz deploy();
    }

    private final static class Impl implements DefineProperties, DeployTinyPlugz {

        private final Map<String, Object> properties;
        private final PluginSourceBuilderImpl builder;
        private final ClassLoader parentCl;

        private Impl(ClassLoader parentCl) {
            if (TinyPlugz.isDeployed()) {
                throw new IllegalStateException("TinyPlugz already deployed");
            }
            this.parentCl = parentCl;
            this.properties = new HashMap<>();
            this.builder = new PluginSourceBuilderImpl();
        }

        @Override
        public DefineProperties withProperty(String name, Object value) {
            this.properties.put(name, value);
            return this;
        }

        @Override
        public DefineProperties withProperties(Map<String, ? extends Object> values) {
            this.properties.putAll(values);
            return this;
        }

        @Override
        public DeployTinyPlugz withPlugins(Consumer<PluginSource> source) {
            source.accept(this.builder);
            return this;
        }

        @Override
        public TinyPlugz deploy() {
            synchronized (INIT_LOCK) {
                final Iterator<TinyPlugz> providers = ServiceLoader
                        .load(TinyPlugz.class, this.parentCl)
                        .iterator();

                final TinyPlugz impl = providers.hasNext()
                        ? providers.next()
                        : new TinyPlugzImpl();

                LOG.debug("Using '{}' TinyPlugz implementation",
                        impl.getClass().getName());
                if (providers.hasNext()) {
                    LOG.warn("Multiple TinyPlugz bindings found on class path");
                    providers.forEachRemaining(provider ->
                            LOG.debug("Ignoring TinyPlugz provider '{}'",
                                    provider.getClass().getName()));
                }
                impl.initializeInstance(this.builder.getPluginUrls(), this.parentCl,
                        this.properties);
                TinyPlugz.instance = impl;
                return impl;
            }
        }
    }
}
