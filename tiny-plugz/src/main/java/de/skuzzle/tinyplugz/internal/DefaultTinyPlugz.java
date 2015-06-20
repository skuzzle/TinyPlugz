package de.skuzzle.tinyplugz.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import de.skuzzle.tinyplugz.DeployListener;
import de.skuzzle.tinyplugz.Options;
import de.skuzzle.tinyplugz.PluginInformation;
import de.skuzzle.tinyplugz.PluginSource;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.util.ElementIterator;
import de.skuzzle.tinyplugz.util.ReflectionUtil;
import de.skuzzle.tinyplugz.util.Require;

final class DefaultTinyPlugz extends TinyPlugz {

    private ServiceLoaderWrapper serviceLoader;
    private DelegateClassLoader pluginClassLoader;

    @Override
    protected final void initialize(PluginSource source,
            ClassLoader parentClassLoader, Map<Object, Object> properties) {
        this.pluginClassLoader = createClassLoader(source, parentClassLoader);
        if (properties.containsKey(Options.SERVICE_LOADER_WRAPPER)) {
            this.serviceLoader = ReflectionUtil.createInstance(
                    properties.get(Options.SERVICE_LOADER_WRAPPER),
                    ServiceLoaderWrapper.class,
                    parentClassLoader);
        } else {
            this.serviceLoader = ServiceLoaderWrapper.getDefault();
        }
    }

    @Override
    public final Collection<PluginInformation> getPluginInformation() {
        return this.pluginClassLoader.getInformation();
    }

    @Override
    protected final Iterator<DeployListener> findDeployListeners(
            ClassLoader pluginClassLoader) {
        return this.serviceLoader.loadService(DeployListener.class, pluginClassLoader);
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
        Require.nonNull(className, "className");
        Require.nonNull(args, "args");
        defaultRunMain(className, args);
    }

    @Override
    public final Optional<URL> getResource(String name) {
        Require.nonNull(name, "name");
        return defaultGetResource(name);
    }

    @Override
    public final ElementIterator<URL> getResources(String name) throws IOException {
        Require.nonNull(name, "name");
        return defaultGetResources(name);
    }

    @Override
    public final <T> ElementIterator<T> getServices(Class<T> type) {
        Require.nonNull(type, "type");
        Require.state(this.serviceLoader != null,"not initialized");
        return this.serviceLoader.loadService(type, this.pluginClassLoader);
    }

    @Override
    public final <T> Optional<T> getFirstService(Class<T> type) {
        Require.nonNull(type, "type");
        return defaultGetFirstService(type);
    }

    @Override
    public final <T> T getService(Class<T> type) {
        Require.nonNull(type, "type");
        return defaultGetService(type);
    }
}
