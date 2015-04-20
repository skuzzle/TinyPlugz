package de.skuzzle.tinyplugz.guice;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

import de.skuzzle.tinyplugz.Require;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzException;

public final class TinyPlugzGuice extends TinyPlugz {

    /**
     * Initialization property to specify additional modules to be used when
     * creating the injector. You man specify any object of type
     * {@code Iterable<Module>} as value for this property.
     *
     * <p>
     * Please note that modules which are registered as services in the host
     * application are loaded automatically and need not to be registered
     * explicitly.
     * </p>
     */
    public static final String ADDITIONAL_MODULES = "tinyplugz.guice.additionalModules";

    /**
     * Property for specifying a parent {@link Injector}. If specified, the
     * Injector created upon initializing will be a child Injector of the given
     * Injector. Otherwise, a new stand-alone Injector will be created.
     */
    public static final String PARENT_INJECTOR = "tinyplugz.guice.parentInjector";

    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugzGuice.class);

    /**
     * Public no argument constructor for java's ServiceLoader. For proper
     * usage, do not instantiate this class manually. See the TinyPlugz
     * documentation to learn about proper usage.
     *
     * @deprecated Do not manually instantiate this class.
     */
    @Deprecated
    public TinyPlugzGuice() {}

    private Injector injector;
    private ClassLoader pluginClassLoader;

    @Override
    protected final void initialize(Set<URL> urls,
            ClassLoader parentClassLoader,
            Map<Object, Object> properties) {

        this.pluginClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
                parentClassLoader);

        final Iterable<Module> appModules = getAdditionalModules(properties);
        final Iterable<Module> pluginModules = getPluginModules();
        final Iterable<Module> internal = getInternalModule();
        final Iterable<Module> modules = Iterators.wrap(internal, appModules,
                pluginModules);
        this.injector = createInjector(properties, modules);
    }

    private Iterable<Module> getInternalModule() {
        final Module internal = new AbstractModule() {

            @Override
            protected void configure() {
                bind(TinyPlugz.class).toInstance(TinyPlugzGuice.this);
                bind(ClassLoader.class).annotatedWith(PluginClassLoader.class)
                        .toInstance(TinyPlugzGuice.this.pluginClassLoader);
            }
        };
        return Iterators.singleIterable(internal);
    }

    private Injector createInjector(Map<Object, Object> props, Iterable<Module> modules) {
        final Injector parent = (Injector) props.get(PARENT_INJECTOR);
        if (parent == null) {
            return Guice.createInjector(modules);
        } else {
            return parent.createChildInjector(modules);
        }
    }

    @SuppressWarnings("unchecked")
    private Iterable<Module> getAdditionalModules(Map<Object, Object> props) {
        final Iterable<Module> modules = (Iterable<Module>) props.get(ADDITIONAL_MODULES);
        if (modules == null) {
            return new Iterable<Module>() {

                @Override
                public Iterator<Module> iterator() {
                    return Collections.emptyIterator();
                }
            };
        }
        return modules;
    }

    private Iterable<Module> getPluginModules() {
        // using the plugin class loader allows to access Services from plugins
        final Iterator<Module> moduleIt = ServiceLoader
                .load(Module.class, this.pluginClassLoader)
                .iterator();

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
        Require.nonNull(type, "type");
        final TypeLiteral<Set<T>> lit = setOf(type);
        final Key<Set<T>> key = Key.get(lit);
        final Set<T> bindings = this.injector.getInstance(key);
        return bindings.iterator();
    }

    @SuppressWarnings("unchecked")
    public static <T> TypeLiteral<Set<T>> setOf(Class<T> type) {
        return (TypeLiteral<Set<T>>) TypeLiteral.get(Types.setOf(type));
    }

    @Override
    public final <T> Optional<T> loadFirstService(Class<T> type) {
        // first, try to resolve multi binding
        final Iterator<T> providers = loadServices(type);
        T provider = null;
        if (!providers.hasNext()) {
            // no multi binding, try for single binding.
            try {
                provider = this.injector.getInstance(type);
            } catch (ConfigurationException e) {
                LOG.warn("Could not get instance for '{}'", type.getName(), e);
                provider = null;
            }
        } else {
            provider = providers.next();
        }
        return Optional.ofNullable(provider);
    }

    @Override
    public final <T> T loadService(Class<T> type) {
        // first, try to resolve multi binding
        final Iterator<T> providers = loadServices(type);
        if (!providers.hasNext()) {
            // no multi binding, try for single binding.
            return this.injector.getInstance(type);
        }
        final T provider = providers.next();
        Require.state(!providers.hasNext(),
                "there are multiple providers for the service '%s'", type.getName());
        return provider;
    }
}
