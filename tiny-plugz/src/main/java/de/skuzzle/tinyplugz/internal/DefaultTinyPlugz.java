package de.skuzzle.tinyplugz.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import de.skuzzle.tinyplugz.PluginSource;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.util.ElementIterator;
import de.skuzzle.tinyplugz.util.Require;

final class DefaultTinyPlugz extends TinyPlugz {

    private final ServiceLoaderWrapper serviceLoader;
    private ClassLoader pluginClassLoader;

    DefaultTinyPlugz() {
        this.serviceLoader = new DefaultServiceLoaderWrapper();
    }

    @Override
    protected final void initialize(PluginSource source,
            ClassLoader parentClassLoader, Map<Object, Object> properties) {
        this.pluginClassLoader = createClassLoader(source, parentClassLoader);
    }

    @Override
    protected final void dispose() {
        defaultDispose();
    }

    @Override
    public final ClassLoader getClassLoader() {
        return this.pluginClassLoader;
    }

    @Override
    public final void runMain(String className, String[] args) {
        defaultRunMain(className, args);
    }

    @Override
    public final Optional<URL> getResource(String name) {
        return defaultGetResource(name);
    }

    @Override
    public final ElementIterator<URL> getResources(String name) throws IOException {
        return defaultGetResources(name);
    }

    @Override
    public final <T> ElementIterator<T> getServices(Class<T> type) {
        Require.nonNull(type, "type");
        return ElementIterator.wrap(
                this.serviceLoader.loadService(type, this.pluginClassLoader));
    }

    @Override
    public final <T> Optional<T> getFirstService(Class<T> type) {
        return defaultGetFirstService(type);
    }

    @Override
    public final <T> T getService(Class<T> type) {
        return defaultGetService(type);
    }
}
