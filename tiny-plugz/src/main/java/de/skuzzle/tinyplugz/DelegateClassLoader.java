package de.skuzzle.tinyplugz;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.tinyplugz.PluginClassLoader.DependencyClassLoader;

/**
 * This ClassLoader allows the application to access classes and resources from
 * any loaded plugin.
 *
 * @author Simon Taddiken
 */
final class DelegateClassLoader extends ClassLoader implements Closeable {

    static {
        registerAsParallelCapable();
    }

    private static final Logger LOG = LoggerFactory.getLogger(DelegateClassLoader.class);

    private final DependencyResolver delegator;

    private DelegateClassLoader(ClassLoader parent, DependencyResolver delegator) {
        super(parent);
        this.delegator = delegator;
    }

    static DelegateClassLoader forPlugins(Collection<URL> urls,
            ClassLoader appClassLoader) {
        Require.nonNull(urls, "urls");
        Require.nonNull(appClassLoader, "parent");

        final Collection<DependencyResolver> plugins = new ArrayList<>(urls.size());
        final DependencyResolver delegator = new DelegateDependencyResolver(plugins);
        for (final URL pluginURL : urls) {
            // Plugin classloaders must be created with the application
            // classloader
            // as parent
            plugins.add(PluginClassLoader.create(pluginURL, appClassLoader, delegator));
        }
        return AccessController.doPrivileged(new PrivilegedAction<DelegateClassLoader>() {

            @Override
            public DelegateClassLoader run() {
                return new DelegateClassLoader(appClassLoader, delegator);
            }
        });
    }

    @Override
    protected final Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {

        final Class<?> c = super.loadClass(name, resolve);
        final ClassLoader loader = c.getClassLoader() == null
                ? this
                : c.getClassLoader();
        if (loader instanceof DependencyClassLoader) {
            LOG.debug("'{}' loaded by '{}' (dependency classloader of '{}')", name,
                    loader,
                    ((DependencyClassLoader) loader).getPluginName());
        } else {
            LOG.debug("'{}' loaded by '{}'", name, loader);
        }
        return c;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
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
    public String toString() {
        return "DelegateClassLoader";
    }
}
