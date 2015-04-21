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

    /**
     * Configuration property for specifying a full qualified name of a class
     * which extends {@link TinyPlugz}. If this property is present, the default
     * lookup for an implementation using the {@link ServiceLoader} is skipped.
     *
     * <p>
     * Note: The presence of this property AND {@link #FORCE_DEFAULT} will raise
     * an exception when {@link DeployTinyPlugz#deploy() deploying}.
     * </p>
     */
    public static final String FORCE_IMPLEMENTATION = "tinyplugz.forceImplementation";

    /**
     * Configuration property for disabling the TinyPlugz implementation lookup
     * and always use the default implementation. Every non-null value will
     * enable this feature.
     *
     * <p>
     * Note: The presence of this property AND {@link #FORCE_IMPLEMENTATION}
     * will raise an exception when {@link DeployTinyPlugz#deploy() deploying}.
     * </p>
     */
    public static final String FORCE_DEFAULT = "tinyplugz.forceDefault";

    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugz.class);
    private static final Object INIT_LOCK = new Object();

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
         * @throws TinyPlugzException When initializing TinyPlugz with the
         *             current configuration fails.
         */
        public TinyPlugz deploy() throws TinyPlugzException;
    }

    private static final class Impl implements DefineProperties, DeployTinyPlugz {

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
            this.properties.put(name, value);
            return this;
        }

        @Override
        public DefineProperties withProperties(
                Map<? extends Object, ? extends Object> values) {
            this.properties.putAll(values);
            return this;
        }

        @Override
        public DeployTinyPlugz withPlugins(Consumer<PluginSource> source) {
            source.accept(this.builder);
            return this;
        }

        @Override
        public TinyPlugz deploy() throws TinyPlugzException {
            validateProperties();
            synchronized (INIT_LOCK) {
                final TinyPlugz impl = getInstance();

                LOG.debug("Using '{}' TinyPlugz implementation",
                        impl.getClass().getName());

                final Collection<URL> plugins = this.builder.getPluginUrls()
                        .collect(Collectors.toList());
                impl.initialize(plugins, this.parentCl,
                        this.properties);
                TinyPlugz.instance = impl;
                return impl;
            }
        }

        private TinyPlugz getInstance() throws TinyPlugzException {
            final TinyPlugzLookUp lookup;
            if (this.properties.get(FORCE_DEFAULT) != null) {
                lookup = new DefaultImplementationTinyPlugzLookup();
            } else if (this.properties.get(FORCE_IMPLEMENTATION) != null) {
                lookup = new StaticTinyPlugzLookup();
            } else {
                lookup = new SPITinyPlugzLookup();
            }
            LOG.debug("Using '{}' for instantiating TinyPlugz",
                    lookup.getClass().getName());
            return lookup.getInstance(this.parentCl, this.properties);
        }

        private void validateProperties() throws TinyPlugzException {
            final Object forceDefault = this.properties.get(FORCE_DEFAULT);
            final Object forceImplementation = this.properties.get(FORCE_IMPLEMENTATION);
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

        /**
         * @deprecated Do not manually instantiate this class.
         */
        @Deprecated
        TinyPlugzImpl() {
            // do not call me!
        }

        @Override
        protected final void initialize(Collection<URL> urls,
                ClassLoader parentClassLoader, Map<Object, Object> properties) {
            this.pluginClassLoader = createClassLoader(urls, parentClassLoader);
        }

        @Override
        public final ClassLoader getClassLoader() {
            return this.pluginClassLoader;
        }

        @Override
        public final void runMain(String className, String[] args)
                throws TinyPlugzException {
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
        public final void contextClassLoaderScope(Runnable r) {
            defaultContextClassLoaderScope(r);
        }

        @Override
        public final <T> Iterator<T> getServices(Class<T> type) {
            return defaultGetServices(type);
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
