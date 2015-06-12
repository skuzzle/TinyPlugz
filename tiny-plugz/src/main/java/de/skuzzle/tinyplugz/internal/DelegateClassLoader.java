package de.skuzzle.tinyplugz.internal;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.tinyplugz.PluginInformation;
import de.skuzzle.tinyplugz.util.ElementIterator;
import de.skuzzle.tinyplugz.util.Require;

/**
 * This ClassLoader allows the application to access classes and resources from
 * any loaded plugin.
 *
 * @author Simon Taddiken
 */
public final class DelegateClassLoader extends ClassLoader implements Closeable {

    static {
        registerAsParallelCapable();
    }

    private static final Logger LOG = LoggerFactory.getLogger(DelegateClassLoader.class);

    private final DependencyResolver delegator;
    private final Collection<PluginInformation> information;

    DelegateClassLoader(ClassLoader parent, DependencyResolver delegator,
            Collection<PluginInformation> information) {
        super(parent);
        this.delegator = delegator;
        this.information = Collections.unmodifiableCollection(information);
    }

    /**
     * Creates a new ClassLoader which provides access to all plugins given by
     * the collection of URLs.
     *
     * @param urls The URLs, each pointing to a plugin to be loaded.
     * @param appClassLoader The ClassLoader to use as parent.
     * @return The created ClassLoader.
     */
    public static DelegateClassLoader forPlugins(Collection<URL> urls,
            ClassLoader appClassLoader) {
        Require.nonNull(urls, "urls");
        Require.nonNull(appClassLoader, "parent");

        final Collection<DependencyResolver> plugins = new ArrayList<>(urls.size());
        final Collection<PluginInformation> information = new ArrayList<>(urls.size());
        final DependencyResolver delegator = new DelegateDependencyResolver(plugins);
        for (final URL pluginURL : urls) {
            // Plugin classloaders must be created with the application
            // classloader
            // as parent
            final PluginClassLoader pluginCl = PluginClassLoader.create(pluginURL,
                    appClassLoader, delegator);
            plugins.add(pluginCl);
            information.add(pluginCl.getPluginInformation());
        }
        return AccessController.doPrivileged(new PrivilegedAction<DelegateClassLoader>() {

            @Override
            public DelegateClassLoader run() {
                return new DelegateClassLoader(appClassLoader, delegator, information);
            }
        });
    }

    /**
     * Information about all loaded plugins.
     *
     * @return A read-only collection of plugin information.
     */
    public final Collection<PluginInformation> getInformation() {
        return this.information;
    }

    @Override
    protected final Class<?> findClass(String name) throws ClassNotFoundException {
        final Class<?> cls = this.delegator.findClass(null, name);
        if (cls == null) {
            throw new ClassNotFoundException(name);
        }
        return cls;
    }

    @Override
    protected final URL findResource(String name) {
        LOG.trace("delegate.findResource('{}')", name);
        return this.delegator.findResource(null, name);
    }

    @Override
    protected final Enumeration<URL> findResources(String name) throws IOException {
        LOG.trace("delegate.findResources('{}')", name);
        final Collection<URL> urls = new ArrayList<>();
        this.delegator.findResources(null, name, urls);
        return ElementIterator.wrap(urls.iterator());
    }

    @Override
    public final void close() throws IOException {
        this.delegator.close();
    }

    @Override
    public final String toString() {
        return "TinyPlugz DelegateClassLoader";
    }
}
