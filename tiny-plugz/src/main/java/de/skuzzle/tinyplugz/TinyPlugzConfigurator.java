package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.tinyplugz.internal.PluginSourceBuilderImpl;
import de.skuzzle.tinyplugz.internal.ServiceLoaderWrapper;
import de.skuzzle.tinyplugz.internal.TinyPlugzLookUp;
import de.skuzzle.tinyplugz.util.ReflectionUtil;
import de.skuzzle.tinyplugz.util.Require;

/**
 * Provides a fluent builder API for configuring an application wide single
 * {@link TinyPlugz} instance.
 *
 * @author Simon Taddiken
 */
public final class TinyPlugzConfigurator {

    /** Default resource name of tiny plugz class path configuration */
    @NonNull
    public static final String TINY_PLUGZ_CONFIG = "tiny-plugz.properties";

    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugz.class);

    /** Lock which synchronizes every non-trivial access to TinyPlugz.instance. */
    protected static final Object DEPLOY_LOCK = new Object();

    private TinyPlugzConfigurator() {
        // hidden constructor
    }

    /**
     * Sets up a {@link TinyPlugz} instance which uses the current thread's
     * context Classloader as parent Classloader.
     * <p>
     * This Classloader will be used for several purposes. First, it serves as
     * parent Classloader for the plugin Classloader which is to be created to
     * access classes and configurations from plugins. Second, the Classloader
     * will be used to look up the TinyPlugz service provider either using the
     * {@link ServiceLoader} or by looking up an explicit implementation class.
     * <p>
     * This method will fail immediately if TinyPlugz already has been
     * configured.
     *
     * @return Fluent builder object for further configuration.
     */
    public static DefineProperties setup() {
        final ClassLoader cl = Require.nonNull(
                Thread.currentThread().getContextClassLoader());
        return new Impl(cl);
    }

    /**
     * Sets up a {@link TinyPlugz} instance which uses the given Classloader as
     * parent Classloader.
     * <p>
     * This Classloader will be used for several purposes. First, it serves as
     * parent Classloader for the plugin Classloader which is to be created to
     * access classes and configurations from plugins. Second, the Classloader
     * will be used to look up the TinyPlugz service provider either using the
     * {@link ServiceLoader} or by looking up an explicit implementation class.
     * <p>
     * This method will fail immediately if TinyPlugz already has been
     * configured.
     *
     * @param parentClassLoader The parent Classloader to use.
     * @return Fluent builder object for further configuration.
     */
    public static DefineProperties setupUsingParent(ClassLoader parentClassLoader) {
        return new Impl(Require.nonNull(parentClassLoader, "parentClassLoader"));
    }

    /**
     * Sets up a {@link TinyPlugz} instance which uses the Classloader which
     * loaded the {@link TinyPlugzConfigurator} class as parent Classloader.
     * <p>
     * This Classloader will be used for several purposes. First, it serves as
     * parent Classloader for the plugin Classloader which is to be created to
     * access classes and configurations from plugins. Second, the Classloader
     * will be used to look up the TinyPlugz service provider either using the
     * {@link ServiceLoader} or by looking up an explicit implementation class.
     * <p>
     * This method will fail immediately if TinyPlugz already has been
     * configured.
     *
     * @return Fluent builder object for further configuration.
     */
    public static DefineProperties setupUsingApplicationClassLoader() {
        final ClassLoader cl = Require.nonNull(
                TinyPlugzConfigurator.class.getClassLoader());
        return new Impl(cl);
    }

    /**
     * Part of the fluent configurator API. Used to define configuration
     * properties and the plugins to be used.
     *
     * @author Simon Taddiken
     */
    public static interface DefineProperties extends DeployTinyPlugz {

        /**
         * Adds properties read from {@value #TINY_PLUGZ_CONFIG} file from the
         * class path.
         *
         * @return A fluent builder object for further configuration.
         * @throws IllegalStateException If the file can not be found.
         */
        DefineProperties withClasspathProperties();

        /**
         * Adds properties read from the class path using the given resource
         * name.
         *
         * @param resourceName Name of the properties file resource.
         * @return A fluent builder object for further configuration.
         * @throws IllegalStateException If the file can not be found.
         */
        DefineProperties withClasspathProperties(String resourceName);

        /**
         * Specifies a single property to insert into the map which will be
         * passed to
         * {@link TinyPlugz#initialize(PluginSource, ClassLoader, Map)} .
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
         * Makes all {@link System#getProperties() system properties} available
         * in the map passed to
         * {@link TinyPlugz#initialize(PluginSource, ClassLoader, Map)}.
         *
         * @return A fluent builder object for further configuration.
         * @since 0.2.0
         */
        DefineProperties withSystemProperties();

        /**
         * Specifies a multiple properties to insert into the map which will be
         * passed to
         * {@link TinyPlugz#initialize(PluginSource, ClassLoader, Map)} .
         *
         * @param values Mappings to add.
         * @return A fluent builder object for further configuration.
         */
        DefineProperties withProperties(Map<? extends Object, ? extends Object> values);

        /**
         * Provides the {@link PluginSourceBuilder} via the given consumer for
         * adding plugins which should be deployed.
         *
         * @param source Consumer for modifying a PluginSourcce.
         * @return A fluent builder object for further configuration.
         */
        DeployTinyPlugz withPlugins(Consumer<PluginSourceBuilder> source);

        /**
         * Uses the given plugin source.
         *
         * @param source The plugins.
         * @return A fluent builder object for further configuration.
         * @since 0.2.0
         */
        DeployTinyPlugz withPlugins(PluginSource source);

        /**
         * {@inheritDoc}
         * <p>
         * When calling this method, only plugins from the folder specified
         * using the option {@link Options#PLUGIN_FOLDER} will be loaded. If
         * this option is not specified, TinyPlugz will be deployed without any
         * plugins.
         *
         * @since 0.2.0
         */
        @Override
        TinyPlugz deploy();
    }

    /**
     * Part of the fluent configurator API. Represents the final step and allows
     * to actually deploy the configured TinyPlugz instance.
     *
     * @author Simon Taddiken
     */
    public interface DeployTinyPlugz {

        /**
         * Creates a new TinyPlugz instance using the configured values. The
         * instance will <b>not</b> be deployed as the unique global instance
         * and can be used fully independent.
         *
         * @return The configured instance.
         * @throws TinyPlugzException When initializing TinyPlugz with the
         *             current configuration fails.
         * @since 0.3.0
         */
        TinyPlugz createInstance();

        /**
         * Finally deploys the {@link TinyPlugz} instance using the configured
         * values. The configured instance will be globally accessible using
         * {@link TinyPlugz#getInstance()}.
         *
         * @return The configured instance.
         * @throws TinyPlugzException When initializing TinyPlugz with the
         *             current configuration fails.
         */
        TinyPlugz deploy();
    }

    private static final class Impl implements DefineProperties, DeployTinyPlugz {

        @NonNull
        private static final Object NON_NULL_VALUE = new Object();

        @NonNull
        private final Map<Object, Object> properties;
        @NonNull
        private final ClassLoader parentCl;
        private PluginSource source;

        private Impl(@NonNull ClassLoader parentCl) {
            this.parentCl = parentCl;
            this.properties = new HashMap<>();
        }

        @Override
        public DefineProperties withClasspathProperties() {
            return withClasspathProperties(TINY_PLUGZ_CONFIG);
        }

        @Override
        public DefineProperties withClasspathProperties(String resourceName) {
            Require.nonNull(resourceName, "resourceName");

            final URL url = Require.nonNullResult(this.parentCl.getResource(resourceName),
                    "ClassLoader.getResource");

            final Properties props = new Properties();

            try (final InputStream in = url.openStream()) {
                props.load(in);
            } catch (final IOException e) {
                throw new IllegalStateException(
                        String.format("Resource <%s> could not be read", resourceName),
                        e);
            }

            this.properties.putAll(props);
            return this;
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
        public DefineProperties withSystemProperties() {
            return withProperties(Require.nonNull(System.getProperties()));
        }

        @Override
        public DefineProperties withProperties(
                Map<? extends Object, ? extends Object> values) {
            Require.nonNull(values, "values");
            this.properties.putAll(values);
            return this;
        }

        @Override
        public DeployTinyPlugz withPlugins(Consumer<PluginSourceBuilder> source) {
            Require.nonNull(source, "source");
            final PluginSourceBuilder builder = PluginSource.builder();
            source.accept(builder);
            this.source = builder.createSource();
            return this;
        }

        @Override
        public DeployTinyPlugz withPlugins(PluginSource source) {
            Require.nonNull(source, "source");
            this.source = source;
            return this;
        }

        @Override
        public TinyPlugz createInstance() {
            validateProperties();
            final TinyPlugz impl = getInstance();

            LOG.debug("Using '{}' TinyPlugz implementation",
                    impl.getClass().getName());

            final PluginSource pluginSource = buildSource();
            logProperties();

            impl.initialize(pluginSource, this.parentCl,
                    Collections.unmodifiableMap(this.properties));
            return impl;
        }

        @Override
        public TinyPlugz deploy() {
            validateProperties();
            synchronized (DEPLOY_LOCK) {
                // additional synchronized check is required here
                Require.state(!TinyPlugz.isDeployed(), "TinyPlugz already deployed");

                final TinyPlugz impl = createInstance();
                TinyPlugz.deploy(impl);
                notifyListeners(impl);
                return impl;
            }
        }

        private PluginSource buildSource() {
            final PluginSourceBuilder builder = new PluginSourceBuilderImpl();
            if (this.properties.get(Options.PLUGIN_FOLDER) != null) {
                final String p = this.properties.get(Options.PLUGIN_FOLDER).toString();
                final Path path = Paths.get(p);

                if (this.source == null) {
                    builder.addAllPluginJars(path);
                } else {
                    builder.include(this.source).addAllPluginJars(path);
                }
            } else if (this.source == null) {
                // plugins given neither by PlginSourceBuilder nor by property
                LOG.warn("TinyPlugz has been configured without specifying any plugin " +
                    "sources");
            } else {
                builder.include(this.source);
            }
            return builder.createSource();
        }

        private void notifyListeners(TinyPlugz tinyPlugz) {
            final Iterator<DeployListener> listeners = tinyPlugz.findDeployListeners(
                    tinyPlugz.getClassLoader());
            Require.nonNullResult(listeners, "TinyPlugz.findDeployListeners");
            while (listeners.hasNext()) {
                final DeployListener next = Require.nonNullResult(listeners.next(),
                        "Iterator.next");
                try {
                    next.initialized(tinyPlugz, this.properties);
                } catch (final RuntimeException e) {
                    LOG.error("DeployListener '{}' threw exception", next, e);
                }
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

            final ServiceLoaderWrapper serviceLoader;
            if (this.properties.get(Options.SERVICE_LOADER_WRAPPER) != null) {
                serviceLoader = ReflectionUtil.createInstance(
                        this.properties.get(Options.SERVICE_LOADER_WRAPPER),
                        ServiceLoaderWrapper.class,
                        this.parentCl);
            } else {
                serviceLoader = ServiceLoaderWrapper.getDefault();
            }

            LOG.debug("Using '{}' for instantiating TinyPlugz",
                    lookup.getClass().getName());
            return lookup.getInstance(this.parentCl, serviceLoader, this.properties);
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

        private void logProperties() {
            if (!LOG.isDebugEnabled() || this.properties.isEmpty()) {
                return;
            }
            final StringBuilder b = new StringBuilder();
            b.append("TinyPlugz configuration options:\n");
            this.properties.forEach((k, v) -> {
                b.append("\t").append(k);
                if (!NON_NULL_VALUE.equals(v)) {
                    b.append(":\t").append(v);
                }
                b.append("\n");
            });
            LOG.debug(b.toString());
        }
    }
}
