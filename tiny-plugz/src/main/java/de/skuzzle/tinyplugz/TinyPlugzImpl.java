package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class TinyPlugzImpl extends TinyPlugz {

    private ClassLoader pluginClassLoader;

    @Override
    protected final void initializeInstance(Set<URL> urls,
            ClassLoader applicationClassLoader, Map<String, Object> properties) {
        this.pluginClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
                applicationClassLoader);
    }

    @Override
    public final ClassLoader getClassLoader() {
        return this.pluginClassLoader;
    }

    @Override
    public final void runMain(String className, String[] args)
            throws TinyPlugzException {
        defaultRunMain(className, args);
    }

    @Override
    public final Optional<URL> getResource(String name) {
        return defaultGetResource(name);
    }

    @Override
    public final Iterator<URL> getResources(String name) throws IOException {
        return defaultGetResources(name);
    }

    @Override
    public final void contextClassLoaderScope(Runnable r) {
        defaultContextClassLoaderScope(r);
    }

    @Override
    public final <T> Iterator<T> loadServices(Class<T> type) {
        return defaultLoadServices(type);
    }

    @Override
    public final <T> Optional<T> loadFirstService(Class<T> type) {
        return defaultLoadFirstService(type);
    }

    @Override
    public final <T> T loadService(Class<T> type) {
        return defaultLoadService(type);
    }
}
