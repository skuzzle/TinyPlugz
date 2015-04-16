package de.skuzzle.tinyplugz.guice;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzException;

public final class GuiceTinyPlugzImpl extends TinyPlugz {

    private Injector injector;
    private ClassLoader pluginClassLoader;

    @Override
    protected void initializeInstance(Set<URL> urls, ClassLoader applicationClassLoader,
            Map<String, Object> properties) {

        this.pluginClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
                applicationClassLoader);

        this.injector = Guice.createInjector(getPluginModules());
    }

    private Iterable<Module> getPluginModules() {
        final Iterator<Module> moduleIt = ServiceLoader.load(Module.class,
                this.pluginClassLoader).iterator();
        return new Iterable<Module>() {

            @Override
            public Iterator<Module> iterator() {
                return moduleIt;
            }
        };
    }


    @Override
    public final void runMain(String className, String[] args) throws TinyPlugzException {
        defaultRunMain(className, args);
    }

    @Override
    public final ClassLoader getClassLoader() {
        return this.pluginClassLoader;
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
        final TypeLiteral<Set<T>> lit = new TypeLiteral<Set<T>>() {};
        final Key<Set<T>> key = Key.get(lit);
        final Set<T> bindings = this.injector.getInstance(key);
        return bindings.iterator();
    }

    @Override
    public final <T> Optional<T> loadFirstService(Class<T> type) {
        return defaultLoadFirstService(type);
    }

    @Override
    public final <T> T loadService(Class<T> type) {
        return this.injector.getInstance(type);
    }
}
