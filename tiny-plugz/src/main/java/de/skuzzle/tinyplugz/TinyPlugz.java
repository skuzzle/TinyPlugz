package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TinyPlugz provides simple runtime classpath extension capabilities by
 * providing a high level API around the java {@link ServiceLoader} and
 * {@link URLClassLoader} classes. Before usage, TinyPlugz must deploy the
 * plugins that should be loaded. After deployment, TinyPlugz can be accessed in
 * a singleton typed manner:
 *
 * <pre>
 * TinyPlugz.deployPlugins(source -&gt; source.addAll(myPluginFolder));
 * final Iterator&lt;MyService&gt; providers = TinyPlugz.getDefault()
 *         .loadServices(MyService.class);
 * </pre>
 *
 * <h2>Usage scenarios</h2> <h3>As child Classloader</h3>
 * <p>
 * This is the preferred usage scenario. TinyPlugz will be configured to use the
 * application's Classloader as parent Classloader. The immediate implication is
 * that the host application does not see classes loaded from plugins but
 * plugins see classes of the host application. The only way for the host
 * application to access features of loaded plugins is to use the java service
 * extension feature through the TinyPlugz API.
 * </p>
 *
 * <p>
 * This method is preferred because it provides a single point of accessing
 * plugin functionality. However, low level access is still possible by using
 * the {@link #getClassLoader() plugin Classloader} directly.
 * </p>
 *
 * <h3>As parent Classloader</h3>
 * <p>
 * In this scenario your whole application will be loaded by TinyPlugz, making
 * TinyPlugz the parent Classloader of your application. This is achieved by
 * TinyPlugz calling the main method of your application. This implies that your
 * application can natively access classes from loaded plugins and all loaded
 * plugins can access classes of your application's static class path. For this
 * scenario, your application needs two {@code main} methods: One which
 * initializes TinyPlugz and a "real" one which is then called by TinyPlugz in
 * the context of the correct Classloader:
 *
 * <pre>
 * // This is the entry point main method of your application
 * public static void main(String[] args) {
 *     TinyPlugz.deployPlugins(source -&gt; source.addAll(yourPluginFolder))
 *             .runMain(&quot;com.your.domain.ClassWithRealMainMethod&quot;, args);
 * }
 * </pre>
 *
 * Please note that this method also replaces the context Classloader of the
 * main thread with TinyPlugz's plugin Classloader.
 * </p>
 *
 * @author Simon Taddiken
 */
public abstract class TinyPlugz {

    private static final Object INIT_LOCK = new Object();
    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugz.class);

    private static volatile TinyPlugz instance;

    /**
     * Gets the single TinyPlugz instance. Please note that before calling this
     * method, TinyPlugz must be configured using
     * {@link #deployPlugins(Consumer)}.
     *
     * @return The TinyPlugz instance.
     */
    public static TinyPlugz getDefault() {
        final TinyPlugz plugz = instance;
        if (plugz == null) {
            throw new IllegalStateException("TinyPlugz has not been initialized");
        }
        return plugz;
    }

    /**
     * Initializes TinyPlugz by loading plugins from the {@link PluginSource}
     * which can be configured using the consumer argument. TinyPlugz will be
     * setup according to the following sequence:
     * <ol>
     * <li>The {@link ServiceLoader} is used to find a {@link TinyPlugz}
     * implementation using the current thread's context Classloader.</li>
     * <li>The first provider which is found will be used.</li>
     * <li>If no provider is found, the default implementation is used.</li>
     * <li>The provider is configured by calling
     * {@link #initializeInstance(Set, ClassLoader)}, passing a set of URLs
     * obtained from the PluginSource and the current thread's context
     * Classloader.</li>
     * <li>The created provider is made accessible through {@link #getDefault()}
     * .</li>
     * </ol>
     *
     * @param source Allows to specify the plugins to load.
     * @return The created TinyPlugz instance.
     */
    public static TinyPlugz deployPlugins(Consumer<PluginSource> source) {
        TinyPlugz plugz = instance;
        if (plugz != null) {
            throw new IllegalStateException("TinyPlugz already initialized");
        }
        synchronized (INIT_LOCK) {
            final PluginSourceBuilderImpl b = new PluginSourceBuilderImpl();
            source.accept(b);
            final ClassLoader appCl = Thread.currentThread().getContextClassLoader();
            instance = initialize(b.getPluginUrls(), appCl);
        }
        return instance;
    }

    private static TinyPlugz initialize(Set<URL> pluginUrls,
            ClassLoader contextClassLoader) {
        final Iterator<TinyPlugz> providers = ServiceLoader
                .load(TinyPlugz.class, contextClassLoader)
                .iterator();

        final TinyPlugz impl = providers.hasNext()
                ? providers.next()
                : new TinyPlugzImpl();

        LOG.debug("Using '{}' TinyPlugz implementation", impl.getClass().getName());
        if (providers.hasNext()) {
            LOG.warn("Multiple TinyPlugz bindings found on class path");
            providers.forEachRemaining(provider ->
                    LOG.debug("Ignoring TinyPlugz provider '{}'",
                            provider.getClass().getName()));
        }
        impl.initializeInstance(pluginUrls, contextClassLoader);
        return impl;
    }

    protected abstract void initializeInstance(Set<URL> urls,
            ClassLoader applicationClassLoader);

    protected abstract void runMain(String cls, String[] args)
            throws TinyPlugzException;

    /**
     * Returns the ClassLoader which can access classes from loaded plugins.
     *
     * @return The plugin ClassLoader.
     */
    public abstract ClassLoader getClassLoader();

    /**
     * Searches for a resource with given name within loaded plugins and the
     * host application.
     *
     * @param name Name of the resource.
     * @return An url to the resource.
     */
    public abstract Optional<URL> getResource(String name);

    /**
     * Finds all the resources with the given name within loaded plugins and the
     * host application.
     *
     * @param name The name of the resource.
     * @return An iterator with resulting resources.
     * @throws IOException If I/O errors occur.
     */
    public abstract Iterator<URL> getResources(String name) throws IOException;

    /**
     * Executes the given {@link Runnable} in the scope of the plugin
     * Classloader. That is, the Classloader which is responsible for loading
     * plugins is set as context Classloader for the current thread. After the
     * runnable has been executed, the original context Classloader is restored.
     *
     * @param r The runnable to execute.
     */
    public abstract void contextClassLoaderScope(Runnable r);

    /**
     * Loads all services of the given type which are accessible from loaded
     * plugins and the host application by using java's {@link ServiceLoader}
     * capabilities.
     *
     * @param type Type of the service to load.
     * @return An iterator of providers for the requested service.
     */
    public abstract <T> Iterator<T> loadServices(Class<T> type);

    /**
     * Loads services of the given type which are accessible from loaded plugins
     * and the host application by using java's {@link ServiceLoader}
     * capabilities. This method only returns the first service or an empty
     * {@link Optional} if no provider for the requested service is found.
     *
     * @param type Type of the service to load.
     * @return The first service which was found.
     */
    public abstract <T> Optional<T> loadFirstService(Class<T> type);

    /**
     * Loads services of the given type which are accessible from loaded plugins
     * and the host application by using java's {@link ServiceLoader}
     * capabilities. This method expects that there exists a single provider for
     * the requested service. An exception will be thrown if no provider has
     * been found or if multiple providers have been found.
     *
     * @param type Type of the service to load.
     * @return The single service.
     */
    public abstract <T> T loadService(Class<T> type);
}
