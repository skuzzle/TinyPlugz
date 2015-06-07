package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.tinyplugz.internal.PluginSourceBuilderImpl;
import de.skuzzle.tinyplugz.internal.TinyPlugzLookUp;
import de.skuzzle.tinyplugz.util.Require;


/**
 * Provides a fluent builder API for configuring an application wide single
 * {@link TinyPlugz} instance.
 *
 * @author Simon Taddiken
 */
public final class TinyPlugzConfigurator {

    /** Default resource name of tiny plugz class path configuration */
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
     * This Classloader will be used for several purposes.
     * First, it serves as parent Classloader for the plugin Classloader which
     * is to be created to access classes and configurations from plugins.
     * Second, the Classloader will be used to look up the TinyPlugz service
     * provider either using the {@link ServiceLoader} or by looking up an
     * explicit implementation class.
     * <p>
     * This method will fail immediately if TinyPlugz already has been
     * configured.
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
     * This Classloader will be used for several purposes.
     * First, it serves as parent Classloader for the plugin Classloader which
     * is to be created to access classes and configurations from plugins.
     * Second, the Classloader will be used to look up the TinyPlugz service
     * provider either using the {@link ServiceLoader} or by looking up an
     * explicit implementation class.
     * <p>
     * This method will fail immediately if TinyPlugz already has been
     * configured.
     *
     * @param parentClassLoader The parent Classloader to use.
     * @return Fluent builder object for further configuration.
     */
    public static DefineProperties setupUsingParent(ClassLoader parentClassLoader) {
        Require.nonNull(parentClassLoader, "parentClassLoader");
        return new Impl(parentClassLoader);
    }

    /**
     * Sets up a {@link TinyPlugz} instance which uses the Classloader which
     * loaded the {@link TinyPlugzConfigurator} class as parent Classloader.
     * <p>
     * This Classloader will be used for several purposes.
     * First, it serves as parent Classloader for the plugin Classloader which
     * is to be created to access classes and configurations from plugins.
     * Second, the Classloader will be used to look up the TinyPlugz service
     * provider either using the {@link ServiceLoader} or by looking up an
     * explicit implementation class.
     * <p>
     * This method will fail immediately if TinyPlugz already has been
     * configured.
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
         * Adds properties read from {@value #TINY_PLUGZ_CONFIG} file from the
         * class path.
         *
         * @return A fluent builder object for further configuration.
         * @throws IllegalStateException If the file can not be found.
         */
        public DefineProperties withClasspathProperties();

        /**
         * Adds properties read from the class path using the given resource
         * name.
         *
         * @param resourceName Name of the properties file resource.
         * @return A fluent builder object for further configuration.
         * @throws IllegalStateException If the file can not be found.
         */
        public DefineProperties withClasspathProperties(String resourceName);

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
         * Makes all {@link System#getProperties() system properties} available
         * in the map passed to
         * {@link TinyPlugz#initialize(Collection, ClassLoader, Map)}.
         *
         * @return A fluent builder object for further configuration.
         * @since 0.2.0
         */
        DefineProperties withSystemProperties();

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
        public DefineProperties withClasspathProperties() {
            return withClasspathProperties(TINY_PLUGZ_CONFIG);
        }

        @Override
        public DefineProperties withClasspathProperties(String resourceName) {
            Require.nonNull(resourceName, "resourceName");

            final URL url = this.parentCl.getResource(resourceName);
            Require.state(url != null, "Resource <%s> not found", resourceName);

            final Properties props = new Properties();

            try (final InputStream in = url.openStream()) {
                props.load(in);
            } catch (IOException e) {
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
            return withProperties(System.getProperties());
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
                        Collections.unmodifiableMap(this.properties));
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
}
