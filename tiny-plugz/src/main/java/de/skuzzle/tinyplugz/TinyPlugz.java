package de.skuzzle.tinyplugz;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import de.skuzzle.tinyplugz.internal.DelegateClassLoader;
import de.skuzzle.tinyplugz.util.Closeables;
import de.skuzzle.tinyplugz.util.ElementIterator;
import de.skuzzle.tinyplugz.util.Require;

/**
 * TinyPlugz provides simple runtime classpath extension capabilities by
 * providing a high level API around the java {@link ServiceLoader} and
 * {@link URLClassLoader} classes. Before usage, TinyPlugz must be configured to
 * specify the plugins which should be loaded. After deploying, TinyPlugz can be
 * accessed like a singleton:
 *
 * <pre>
 * TinyPlugzConfigurator.setup()
 *         .withProperty(&quot;key&quot;, value) // some TinyPlugz implementations might
 *                                     // need additional properties
 *         .withPlugins(source -&gt; source.addAll(pluginFolder))
 *         .deploy();
 *
 * final Iterator&lt;MyService&gt; providers = TinyPlugz.getInstance()
 *         .loadServices(MyService.class);
 * </pre>
 *
 * <h2>Usage scenario</h2>
 * <p>
 * TinyPlugz will be configured to use the application's Classloader as parent
 * Classloader. The immediate implication is that the host application does not
 * see classes loaded from plugins but plugins see classes of the host
 * application. The only way for the host application to access features of
 * loaded plugins is to use the java service extension feature through the
 * TinyPlugz API.
 * <p>
 * This method provides a single point of accessing plugin functionality.
 * However, low level access is still possible by using the
 * {@link #getClassLoader() plugin Classloader} directly.
 *
 * <h2>Deploy time Extensibility</h2>
 * <p>
 * The {@link TinyPlugz} instance returned by {@link #getInstance()} is
 * automatically determined by using java's {@link ServiceLoader} class. It will
 * look for an registered service provider for the type TinyPlugz and use the
 * first encountered provider. If no provider is found, the default
 * implementation will be used.
 *
 * @author Simon Taddiken
 */
public abstract class TinyPlugz {

    private static volatile TinyPlugz instance;

    /**
     * Gets the single TinyPlugz instance.
     *
     * @return The TinyPlugz instance.
     */
    public static TinyPlugz getInstance() {
        final TinyPlugz plugz = instance;
        Require.state(plugz != null, "TinyPlugz has not been initialized");
        return plugz;
    }

    static void deploy(TinyPlugz instance) {
        // visible for TinyPlugzConfigurator
        TinyPlugz.instance = instance;
    }

    /**
     * Checks whether TinyPlugz is currently deployed and is thus accessible
     * using {@link #getInstance()}.
     *
     * @return Whether {@link TinyPlugz} is deployed.
     */
    public static boolean isDeployed() {
        return instance != null;
    }

    /**
     * Undeploys the global {@link TinyPlugz} instance and calls its
     * {@link #dispose()} method. This method will fail if the instance on which
     * it is called is not the deployed instance.
     */
    public final void undeploy() {
        synchronized (TinyPlugzConfigurator.DEPLOY_LOCK) {
            Require.state(isDeployed(),
                    "Can not undeploy TinyPlugz: no instance deployed");
            final TinyPlugz plugz = instance;
            Require.state(plugz == this,
                    "Undeploy called on an instance which was not the deployed one");
            instance = null;
            plugz.dispose();
        }
    }

    /**
     * This method is called by the TinyPlugz runtime right after instantiation
     * of this instance. The first thing implementations of this method should
     * do is to store a reference to the passed properties in order to make it
     * available through {@link #getProperties()} during the initialization
     * process.
     *
     * @param source The plugins to load.
     * @param parentClassLoader The Classloader to use as parent for the plugin
     *            Classloader.
     * @param properties Additional configuration parameters.
     * @throws TinyPlugzException When initializing failed.
     */
    protected abstract void initialize(PluginSource source,
            ClassLoader parentClassLoader, Map<Object, Object> properties);

    /**
     * Gets a read-only view of the properties that have been passed to deploy.
     * The map must be available during initializing, so plugin service can read
     * them for own initialization pruposes.
     *
     * @return The deployment properties.
     * @since 0.4.0
     */
    public abstract Map<Object, Object> getProperties();

    /**
     * Looks up {@link DeployListener} to be notified right after this instance
     * has been deployed by the {@link TinyPlugzConfigurator} class. The
     * listeners are retrieved querying Java's {@link ServiceLoader} for the
     * service {@code DeployListener.class}.
     *
     * @param pluginClassLoader The ClassLoader for accessing services from
     *            plugins.
     * @return An iterator of available DeployListeners.
     */
    protected abstract Iterator<DeployListener> findDeployListeners(
            ClassLoader pluginClassLoader);

    /**
     * Called upon {@link #undeploy() undeploy} to release resources.
     */
    protected abstract void dispose();

    /**
     * Default behavior for {@link #dispose()}: Calls {@link Closeable#close()
     * close} if the Classloader returned by {@link #getClassLoader()} is an
     * instance of {@link Closeable}.
     */
    protected final void defaultDispose() {
        if (getClassLoader() instanceof Closeable) {
            final Closeable cl = (Closeable) getClassLoader();
            Closeables.safeClose(cl);
        }
    }

    /**
     * Gets a collection of information about all loaded plugins. The order in
     * which information objects are returned is undefined.
     *
     * @return A read-only collection of plugin information.
     */
    public abstract Collection<PluginInformation> getPluginInformation();

    /**
     * Gets the information for the plugin with given name if such a plugin is
     * loaded. Otherwise returns an empty {@link Optional}.
     *
     * @param pluginName Name of the plugin.
     * @return The information about that plugin.
     * @since 0.4.0
     */
    public abstract Optional<PluginInformation> getPluginInformation(String pluginName);

    /**
     * Returns the ClassLoader which can access classes from loaded plugins.
     * This gives the caller low level access to the plugins. If possible, use
     * the abstractions of the {@link TinyPlugz} class instead.
     *
     * @return The plugin ClassLoader.
     */
    public abstract ClassLoader getClassLoader();

    /**
     * Creates a {@link ClassLoader} which accesses the given collection of
     * plugins.
     *
     * @param source The plugins.
     * @param parent The parent ClassLoader.
     * @return The created ClassLoader.
     */
    protected final DelegateClassLoader createClassLoader(PluginSource source,
            ClassLoader parent) {
        final Stream<URL> urls = Require.nonNullResult(source.getPluginURLs(),
                "pluginSource.getPluginURLs");
        return DelegateClassLoader.forPlugins(urls, parent);
    }

    /**
     * Searches for a resource with given name within loaded plugins and the
     * host application.
     *
     * @param name Name of the resource.
     * @return An url to the resource.
     */
    public abstract Optional<URL> getResource(String name);

    /**
     * Default implementation for {@link #getResource(String)} building upon
     * result of {@link #getClassLoader()}.
     *
     * @param name Name of the resource.
     * @return An url to the resource.
     */
    protected final Optional<URL> defaultGetResource(String name) {
        return Optional.ofNullable(getClassLoader().getResource(name));
    }

    /**
     * Finds all the resources with the given name within loaded plugins and the
     * host application.
     *
     * @param name The name of the resource.
     * @return An iterator with resulting resources.
     * @throws IOException If I/O errors occur.
     * @see ElementIterator
     */
    public abstract ElementIterator<URL> getResources(String name) throws IOException;

    /**
     * Default implementation for {@link #getResources(String)} building upon
     * result of {@link #getClassLoader()}.
     *
     * @param name The name of the resource.
     * @return An iterator with resulting resources.
     * @throws IOException If I/O errors occur.
     */
    protected final ElementIterator<URL> defaultGetResources(String name)
            throws IOException {
        final Enumeration<URL> e = getClassLoader().getResources(name);
        return ElementIterator.wrap(e);
    }

    /**
     * Checks whether there is at least one provider available for the given
     * service. This method is shorthand for
     * {@code getFirstService(service).isPresent()}.
     *
     * @param service The service to check for.
     * @return Whether at least one provider exists.
     */
    public final boolean isServiceAvailable(Class<?> service) {
        return getFirstService(service).isPresent();
    }

    /**
     * Loads all services of the given type which are accessible from loaded
     * plugins and the host application by using java's {@link ServiceLoader}
     * capabilities.
     *
     * @param <T> The type of the service provider interface.
     * @param type Type of the service to load.
     * @return An iterator of providers for the requested service.
     */
    public abstract <T> ElementIterator<T> getServices(Class<T> type);

    /**
     * Loads services of the given type which are accessible from loaded plugins
     * and the host application by using java's {@link ServiceLoader}
     * capabilities. This method only returns the first service or an empty
     * {@link Optional} if no provider for the requested service is found.
     *
     * @param <T> The type of the service provider interface.
     * @param type Type of the service to load.
     * @return The first service which was found.
     */
    public abstract <T> Optional<T> getFirstService(Class<T> type);

    /**
     * Default implementation for {@link #getFirstService(Class)} building upon
     * result of {@link #getServices(Class)}.
     *
     * @param <T> The type of the service provider interface.
     * @param type Type of the service to load.
     * @return The first service which was found.
     */
    protected final <T> Optional<T> defaultGetFirstService(Class<T> type) {
        final Iterator<T> services = getServices(type);
        return services.hasNext()
                ? Optional.of(services.next())
                : Optional.empty();
    }

    /**
     * Loads services of the given type which are accessible from loaded plugins
     * and the host application by using java's {@link ServiceLoader}
     * capabilities. This method expects that there exists a single provider for
     * the requested service. An exception will be thrown if no provider has
     * been found or if multiple providers have been found.
     *
     * @param <T> The type of the service provider interface.
     * @param type Type of the service to load.
     * @return The single service.
     * @throws IllegalStateException If no provider or if more than one provider
     *             of given type is available.
     */
    public abstract <T> T getService(Class<T> type);

    /**
     * Default implementation of {@link #getService(Class)} building upon result
     * of {@link #getServices(Class)}.
     *
     * @param <T> The type of the service provider interface.
     * @param type Type of the service to load.
     * @return The single service.
     * @throws IllegalStateException If no provider or if more than one provider
     *             of given type is available.
     */
    protected final <T> T defaultGetService(Class<T> type) {
        final Iterator<T> services = getServices(type);
        Require.state(services.hasNext(), "no provider for service '%s' found",
                type.getName());

        final T first = services.next();
        Require.state(!services.hasNext(),
                "there are multiple providers for the service '%s'", type.getName());

        return first;
    }
}
