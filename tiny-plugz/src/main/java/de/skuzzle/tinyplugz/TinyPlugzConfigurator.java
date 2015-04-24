package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    protected static final Object DEPLOY_LOCK = new Object();

    private TinyPlugzConfigurator() {
        // hidden constructor
    }

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

    /**
     * Part of the fluent configurator API. Used to define configuration
     * properties and the plugins to be used.
     *
     * @author Simon Taddiken
     */
    public static interface DefineProperties {

        /**
         * Specifies a single property to insert into the map which will be
         * passed to
         * {@link TinyPlugz#initialize(java.util.Collection, ClassLoader, Map)}
         * .
         *
         * @param name Name of the property.
         * @param value Value of the property.
         * @return A fluent builder object for further configuration.
         */
        DefineProperties withProperty(String name, Object value);

        /**
         * Specifies a property without value. It will automatically get
         * assigned a non-null value.
         *
         * @param name Name of the property.
         * @return A fluent builder object for further configuration.
         */
        DefineProperties withProperty(String name);

        /**
         * Specifies a multiple properties to insert into the map which will be
         * passed to
         * {@link TinyPlugz#initialize(java.util.Collection, ClassLoader, Map)}
         * .
         *
         * @param values Mappings to add.
         * @return A fluent builder object for further configuration.
         */
        DefineProperties withProperties(Map<? extends Object, ? extends Object> values);

        /**
         * Provides the {@link PluginSource} via the given consumer for adding
         * plugins which should be deployed.
         *
         * @param source Consumer for modifying a PluginSourcce.
         * @return A fluent builder object for further configuration.
         */
        DeployTinyPlugz withPlugins(Consumer<PluginSource> source);
    }

    /**
     * Part of the fluent configurator API. Represents the final step and allows
     * to actually deploy the configured TinyPlugz instance.
     *
     * @author Simon Taddiken
     */
    public interface DeployTinyPlugz {

        /**
         * Finally deploys the {@link TinyPlugz} instance using the the
         * configured values. The configured instance will be globally
         * accessible using {@link TinyPlugz#getInstance()}.
         *
         * @return The configured instance.
         * @throws TinyPlugzException When initializing TinyPlugz with the
         *             current configuration fails.
         */
        public TinyPlugz deploy();
    }

    private static final class Impl implements DefineProperties, DeployTinyPlugz {

        private static final Object NON_NULL_VALUE = new Object();

        private final Map<Object, Object> properties;
        private final PluginSourceBuilderImpl builder;
        private final ClassLoader parentCl;

        private Impl(ClassLoader parentCl) {
            Require.state(!TinyPlugz.isDeployed(), "TinyPlugz already deployed");
            this.parentCl = parentCl;
            this.properties = new HashMap<>();
            this.builder = new PluginSourceBuilderImpl();
        }

        @Override
        public DefineProperties withProperty(String name, Object value) {
            Require.nonNull(name, "name");
            this.properties.put(name, value);
            return this;
        }

        @Override
        public DefineProperties withProperty(String name) {
            return withProperty(name, NON_NULL_VALUE);
        }

        @Override
        public DefineProperties withProperties(
                Map<? extends Object, ? extends Object> values) {
            Require.nonNull(values, "values");
            this.properties.putAll(values);
            return this;
        }

        @Override
        public DeployTinyPlugz withPlugins(Consumer<PluginSource> source) {
            Require.nonNull(source, "source");
            source.accept(this.builder);
            return this;
        }

        @Override
        public TinyPlugz deploy() {
            validateProperties();
            synchronized (DEPLOY_LOCK) {
                // additional synchronized check is required
                Require.state(!TinyPlugz.isDeployed(), "TinyPlugz already deployed");

                final TinyPlugz impl = getInstance();

                LOG.debug("Using '{}' TinyPlugz implementation",
                        impl.getClass().getName());

                final Collection<URL> plugins = this.builder.getPluginUrls()
                        .collect(Collectors.toList());
                impl.initialize(plugins, this.parentCl,
                        this.properties);
                TinyPlugz.deploy(impl);
                return impl;
            }
        }

        private TinyPlugz getInstance() {
            final TinyPlugzLookUp lookup;
            if (this.properties.get(Options.FORCE_DEFAULT) != null) {
                lookup = TinyPlugzLookUp.DEFAULT_INSTANCE_STRATEGY;
            } else if (this.properties.get(Options.FORCE_IMPLEMENTATION) != null) {
                lookup = TinyPlugzLookUp.STATIC_STRATEGY;
            } else {
                lookup = TinyPlugzLookUp.SPI_STRATEGY;
            }
            LOG.debug("Using '{}' for instantiating TinyPlugz",
                    lookup.getClass().getName());
            return lookup.getInstance(this.parentCl, this.properties);
        }

        private void validateProperties() {
            final Object forceDefault = this.properties.get(Options.FORCE_DEFAULT);
            final Object forceImplementation = this.properties.get(
                    Options.FORCE_IMPLEMENTATION);

            if (forceDefault != null && forceImplementation != null) {
                throw new TinyPlugzException("Can not use 'FORCE_IMPLEMENTATION' " +
                            "together with 'FORCE_DEFAULT'");
            }
        }
    }

    /**
     * Default TinyPlugz implementation which will be used if no other service
     * provider is found. It relies solely on the defaultXXX methods of the
     * TinyPlugz class.
     *
     * @author Simon Taddiken
     */
    static final class TinyPlugzImpl extends TinyPlugz {

        private ClassLoader pluginClassLoader;

        @Override
        protected final void initialize(Collection<URL> urls,
                ClassLoader parentClassLoader, Map<Object, Object> properties) {
            this.pluginClassLoader = createClassLoader(urls, parentClassLoader);
        }

        @Override
        protected final void dispose() {
            defaultDispose();
        }

        @Override
        public final ClassLoader getClassLoader() {
            return this.pluginClassLoader;
        }

        @Override
        public final void runMain(String className, String[] args) {
            defaultRunMain(className, args);
        }

        @Override
        public final Optional<URL> getResource(String name) {
            return defaultGetResource(name);
        }

        @Override
        public final Iterator<URL> getResources(String name) throws IOException {
            return defaultGetResources(name);
        }

        @Override
        public final void contextClassLoaderScope(ContextAction action) {
            defaultContextClassLoaderScope(action);
        }

        @Override
        public final <T> Iterator<T> getServices(Class<T> type) {
            Require.nonNull(type, "type");
            return ServiceLoader.load(type, getClassLoader()).iterator();
        }

        @Override
        public final <T> Optional<T> getFirstService(Class<T> type) {
            return defaultGetFirstService(type);
        }

        @Override
        public final <T> T getService(Class<T> type) {
            return defaultGetService(type);
        }
    }
}
