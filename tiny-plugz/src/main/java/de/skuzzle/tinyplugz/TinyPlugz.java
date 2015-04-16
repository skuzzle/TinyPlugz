package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

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

    static volatile TinyPlugz instance;

    /**
     * Gets the single TinyPlugz instance.
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

    static boolean isDeployed() {
        return instance != null;
    }

    protected abstract void initializeInstance(Set<URL> urls,
            ClassLoader applicationClassLoader, Map<String, Object> properties);

    public abstract void runMain(String cls, String[] args)
            throws TinyPlugzException;

    protected final void defaultRunMain(String className, String[] args)
            throws TinyPlugzException {
        try {
            Thread.currentThread().setContextClassLoader(getClassLoader());
            final Class<?> cls = getClassLoader().loadClass(className);
            final Method method = cls.getMethod("main", new Class<?>[] { String[].class });

            boolean bValidModifiers = false;
            boolean bValidVoid = false;

            if (method != null) {
                method.setAccessible(true); // Disable IllegalAccessException
                int nModifiers = method.getModifiers(); // main() must be
                                                        // "public static"
                bValidModifiers = Modifier.isPublic(nModifiers) &&
                    Modifier.isStatic(nModifiers);
                Class<?> clazzRet = method.getReturnType(); // main() must be
                                                            // "void"
                bValidVoid = (clazzRet == void.class);
            }
            if (method == null || !bValidModifiers || !bValidVoid) {
                throw new TinyPlugzException(
                        "The main() method in class \"" + cls.getName() + "\" not found.");
            }

            // Invoke method.
            // Crazy cast "(Object)args" because param is: "Object... args"
            method.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw new TinyPlugzException(e.getTargetException());
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | ClassNotFoundException e) {
            throw new TinyPlugzException(e);
        }
    }

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
     */
    public abstract Iterator<URL> getResources(String name) throws IOException;

    protected final Iterator<URL> defaultGetResources(String name) throws IOException {
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
     * Executes the given {@link Runnable} in the scope of the plugin
     * Classloader. That is, the Classloader which is responsible for loading
     * plugins is set as context Classloader for the current thread. After the
     * runnable has been executed, the original context Classloader is restored.
     *
     * @param r The runnable to execute.
     */
    public abstract void contextClassLoaderScope(Runnable r);

    protected final void defaultContextClassLoaderScope(Runnable r) {
        final Thread current = Thread.currentThread();
        final ClassLoader contextCl = current.getContextClassLoader();
        try {
            current.setContextClassLoader(getClassLoader());
            r.run();
        } finally {
            current.setContextClassLoader(contextCl);
        }
    }

    /**
     * Loads all services of the given type which are accessible from loaded
     * plugins and the host application by using java's {@link ServiceLoader}
     * capabilities.
     *
     * @param type Type of the service to load.
     * @return An iterator of providers for the requested service.
     */
    public abstract <T> Iterator<T> loadServices(Class<T> type);

    protected final <T> Iterator<T> defaultLoadServices(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }

        return ServiceLoader.load(type, getClassLoader()).iterator();
    }

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

    protected final <T> Optional<T> defaultLoadFirstService(Class<T> type) {
        final Iterator<T> services = loadServices(type);
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
    public abstract <T> T loadService(Class<T> type);

    protected final <T> T defaultLoadService(Class<T> type) {
        final Iterator<T> services = loadServices(type);
        if (!services.hasNext()) {
            throw new IllegalStateException(String.format(
                    "no provider for service '%s' found", type));
        }
        final T first = services.next();
        if (services.hasNext()) {
            throw new IllegalStateException(String.format(
                    "there are multiple providers for the service '%s'", type));
        }

        return first;
    }
}
