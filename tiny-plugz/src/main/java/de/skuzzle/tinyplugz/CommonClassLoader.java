package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

class CommonClassLoader extends ClassLoader {

    private Collection<PluginClassLoader> plugins;

    private CommonClassLoader(ClassLoader parent) {
        super(parent);
    }

    public static CommonClassLoader forPlugins(Collection<URL> urls, ClassLoader parent) {
        Require.nonNull(urls, "urls");
        Require.nonNull(parent, "parent");

        final CommonClassLoader commonClassLoader = AccessController.doPrivileged(
                new PrivilegedAction<CommonClassLoader>() {

            @Override
            public CommonClassLoader run() {
                return new CommonClassLoader(parent);
            }
        });

        final Collection<PluginClassLoader> plugins = new ArrayList<>(urls.size());
        for (final URL pluginURL : urls) {
            plugins.add(PluginClassLoader.create(pluginURL, commonClassLoader));
        }
        commonClassLoader.plugins = plugins;
        return commonClassLoader;
    }

    @Override
    protected final Class<?> findClass(String name) throws ClassNotFoundException {
        for (final PluginClassLoader pluginCl : this.plugins) {
            try {
                return pluginCl.findClass(name);
            } catch (ClassNotFoundException ignore) {}
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected final URL findResource(String name) {
        for (final PluginClassLoader pluginCl : this.plugins) {
            final URL url = pluginCl.findResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    @Override
    protected final Enumeration<URL> findResources(String name) throws IOException {
        final Collection<URL> urls = new ArrayList<>();
        for (PluginClassLoader pluginCl : this.plugins) {
            addAll(urls, pluginCl.findResources(name));
        }
        return ElementIterator.wrap(urls.iterator());
    }

    private <T> void addAll(Collection<T> target, Enumeration<T> elements) {
        while (elements.hasMoreElements()) {
            target.add(elements.nextElement());
        }
    }

}
