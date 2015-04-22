package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TinyPlugz provides simple runtime classpath extension capabilities by
 * providing a high level API around the java {@link ServiceLoader} and
 * {@link URLClassLoader} classes. Before usage, TinyPlugz must be configured to
 * specify the plugins which should be loaded. After deploying, TinyPlugz can be
 * accessed in a singleton typed manner:
 *
 * <pre>
 * TinyPlugzConfigurator.setup()
 *         .withProperty(&quot;key&quot;, value) // some TinyPlugz implementations might
 *                                     // need additional properties
 *         .withPlugins(source -&gt; source.addAll(pluginFolder))
 *         .deploy();
 *
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
 * <h3>As parent Classloader (container mode)</h3>
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
 *     TinyPlugzConfigurator.setup()
 *             .withProperty(&quot;key&quot;, value)
 *             .withPlugins(source -&gt; source.addAll(pluginFolder))
 *             .deploy()
 *             .runMain(&quot;com.your.domain.ClassWithRealMainMethod&quot;, args);
 * }
 * </pre>
 *
 * Please note that this method also replaces the context Classloader of the
 * main thread with TinyPlugz's plugin Classloader.
 * </p>
 *
 * <h2>Deploytime Extensibility</h2>
 * <p>
 * The {@link TinyPlugz} instance returned by {@link #getDefault()} is
 * automatically determined by using java's {@link ServiceLoader} class. It will
 * look for an registered service provider for the type TinyPlugz and use the
 * first encountered provider. If no provider is found, the default
 * implementation will be used.
 * </p>
 *
 * @author Simon Taddiken
 */
public abstract class TinyPlugz {

    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugz.class);

    private static volatile TinyPlugz instance;

    /**
     * Gets the single TinyPlugz instance.
     *
     * @return The TinyPlugz instance.
     */
    public static TinyPlugz getDefault() {
        final TinyPlugz plugz = instance;
        Require.state(plugz != null, "TinyPlugz has not been initialized");
        return plugz;
    }

    static void deploy(TinyPlugz instance) {
        TinyPlugz.instance = instance;
    }

    static boolean isDeployed() {
        // visible for the TinyPlugzConfigurator.
        return instance != null;
    }

    /**
     * This method is called by the TinyPlugz runtime right after instantiation
     * of this instance.
     *
     * @param urls The urls pointing to loadable plugins.
     * @param parentClassLoader The Classloader to use as parent.
     * @param properties Additional configuration parameters.
     * @throws TinyPlugzException When initializing failed.
     */
    protected abstract void initialize(Collection<URL> urls,
            ClassLoader parentClassLoader, Map<Object, Object> properties)
            throws TinyPlugzException;

    /**
     * Executes a main method in the context of the plugin ClassLoader. This
     * method uses the plugin ClassLoader to load the class with given
     * {@code className} and then searches for a
     * {@code public static void main(String[] args)} method to execute.
     * Additionally, the plugin ClassLoader is set as context ClassLoader for
     * the current thread.
     *
     * <p>
     * Using this method it is possible to use TinyPlugz as an execution
     * container for the whole application, because all subsequently loaded
     * classes will have access to the plugin ClassLoader.
     * </p>
     *
     * @param className Name of the class which contains the main method.
     * @param args Arguments to pass to the main method.
     * @throws TinyPlugzException If loading the class or calling it's main
     *             method fails.
     */
    public abstract void runMain(String className, String[] args)
            throws TinyPlugzException;

    /**
     * Default implementation for {@link #runMain(String, String[])}.
     *
     * @param className Name of the class which contains the main method.
     * @param args Arguments to pass to the main method.
     * @throws TinyPlugzException If loading the class or calling it's main
     *             method fails.
     */
    protected final void defaultRunMain(String className, String[] args)
            throws TinyPlugzException {
        Require.nonNull(className, "className");
        Require.nonNull(args, "args");

        try {
            Thread.currentThread().setContextClassLoader(getClassLoader());
            final Class<?> cls = getClassLoader().loadClass(className);
            final Method method = cls.getMethod("main",
                    new Class<?>[] { String[].class });

            boolean bValidModifiers = false;
            boolean bValidVoid = false;

            if (method != null) {
                method.setAccessible(true);
                int nModifiers = method.getModifiers();
                bValidModifiers = Modifier.isPublic(nModifiers) &&
                    Modifier.isStatic(nModifiers);
                Class<?> clazzRet = method.getReturnType();

                bValidVoid = (clazzRet == void.class);
            }
            if (!bValidModifiers || !bValidVoid) {
                throw new TinyPlugzException(String.format(
                        "The main() method in class '%s' not found.", cls.getName()));
            }

            // Crazy cast "(Object)args" because param is: "Object... args"
            method.invoke(null, (Object) args);
        } catch (InvocationTargetException | NoSuchMethodException | SecurityException
                | IllegalAccessException | IllegalArgumentException
                | ClassNotFoundException e) {
            throw new TinyPlugzException(e);
        }
    }

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
     * @param plugins The plugins.
     * @param parent The parent ClassLoader.
     * @return The created ClassLoader.
     */
    protected final ClassLoader createClassLoader(Collection<URL> plugins,
            ClassLoader parent) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                if (LOG.isDebugEnabled()) {
                    for (final URL url : plugins) {
                        LOG.debug("Loading plugin from '{}'", url);
                    }
                }
                return new URLClassLoader(plugins.toArray(new URL[plugins.size()]),
                            parent);
            }
        });
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
        Require.nonNull(name, "name");
        return Optional.ofNullable(getClassLoader().getResource(name));
    }

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
     * Default implementation for {@link #getResources(String)} building upon
     * result of {@link #getClassLoader()}.
     *
     * @param name The name of the resource.
     * @return An iterator with resulting resources.
     * @throws IOException If I/O errors occur.
     */
    protected final Iterator<URL> defaultGetResources(String name) throws IOException {
        Require.nonNull(name, "name");
        final Enumeration<URL> e = getClassLoader().getResources(name);
        return new Iterator<URL>() {

            @Override
            public boolean hasNext() {
                return e.hasMoreElements();
            }

            @Override
            public URL next() {
                return e.nextElement();
            }
        };
    }

    /**
     * Executes the given {@link ContextAction#perform() ContextAction} in the
     * scope of the plugin Classloader. That is, the Classloader which is
     * responsible for loading plugins is set as context Classloader for the
     * current thread. After the action has been executed, the original context
     * Classloader is restored.
     *
     * @param action The action to execute.
     */
    public abstract void contextClassLoaderScope(ContextAction action);

    /**
     * Default implementation for
     * {@link #contextClassLoaderScope(ContextAction)} building upon result of
     * {@link #getClassLoader()}.
     *
     * @param action The action to execute.
     */
    protected final void defaultContextClassLoaderScope(ContextAction action) {
        Require.nonNull(action, "action");
        final Thread current = Thread.currentThread();
        final ClassLoader contextCl = current.getContextClassLoader();
        try {
            current.setContextClassLoader(getClassLoader());
            action.perform();
        } finally {
            current.setContextClassLoader(contextCl);
        }
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
     * @param type Type of the service to load.
     * @return An iterator of providers for the requested service.
     */
    public abstract <T> Iterator<T> getServices(Class<T> type);

    /**
     * Loads services of the given type which are accessible from loaded plugins
     * and the host application by using java's {@link ServiceLoader}
     * capabilities. This method only returns the first service or an empty
     * {@link Optional} if no provider for the requested service is found.
     *
     * @param type Type of the service to load.
     * @return The first service which was found.
     */
    public abstract <T> Optional<T> getFirstService(Class<T> type);

    /**
     * Default implementation for {@link #getFirstService(Class)} building upon
     * result of {@link #getServices(Class)}.
     *
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
     * @param type Type of the service to load.
     * @return The single service.
     */
    public abstract <T> T getService(Class<T> type);

    /**
     * Default implementation of {@link #getService(Class)} building upon result
     * of {@link #getServices(Class)}.
     *
     * @param type Type of the service to load.
     * @return The single service.
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
