package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

class CommonClassLoader extends ClassLoader implements DependencyResolver {

    private Collection<PluginClassLoader> plugins;

    private CommonClassLoader(ClassLoader parent) {
        super(parent);
    }

    public static CommonClassLoader forPlugins(Collection<URL> urls,
            ClassLoader appClassLoader) {
        Require.nonNull(urls, "urls");
        Require.nonNull(appClassLoader, "parent");

        final CommonClassLoader commonClassLoader = AccessController.doPrivileged(
                new PrivilegedAction<CommonClassLoader>() {

            @Override
            public CommonClassLoader run() {
                return new CommonClassLoader(appClassLoader);
            }
        });

        final Collection<PluginClassLoader> plugins = new ArrayList<>(urls.size());
        for (final URL pluginURL : urls) {
            plugins.add(PluginClassLoader.create(pluginURL, appClassLoader,
                    commonClassLoader));
        }
        commonClassLoader.plugins = plugins;
        return commonClassLoader;
    }

    @Override
    protected final Class<?> findClass(String name) throws ClassNotFoundException {
        final Class<?> cls = findClass(null, name);
        if (cls == null) {
            throw new ClassNotFoundException(name);
        }
        return cls;
    }

    @Override
    protected final URL findResource(String name) {
        return findResource(null, name);
    }

    @Override
    protected final Enumeration<URL> findResources(String name) throws IOException {
        final Collection<URL> urls = new ArrayList<>();
        findResources(null, name, urls);
        return ElementIterator.wrap(urls.iterator());
    }

    @Override
    public final Class<?> findClass(PluginClassLoader requestor, String name) {
        for (final PluginClassLoader pluginCl : this.plugins) {
            final Class<?> cls = pluginCl.findClass(requestor, name);
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }

    @Override
    public final URL findResource(PluginClassLoader requestor, String name) {
        for (final PluginClassLoader pluginCl : this.plugins) {
            final URL url = pluginCl.findResource(requestor, name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    @Override
    public final void findResources(PluginClassLoader requestor, String name,
            Collection<URL> target) throws IOException {

        for (final PluginClassLoader pluginCl : this.plugins) {
            pluginCl.findResources(requestor, name, target);
        }
    }

}
