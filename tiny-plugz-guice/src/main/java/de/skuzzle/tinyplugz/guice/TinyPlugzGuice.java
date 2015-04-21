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
import com.google.inject.name.Names;
import com.google.inject.util.Types;

import de.skuzzle.tinyplugz.Require;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzException;

/**
 * TinyPlugz implementation building upon google Guice and its Multibinding
 * extension. When {@link #initialize(Set, ClassLoader, Map) initialize} is
 * called, this implementation asks the {@link ServiceLoader} for providers
 * implementing Guice's {@link Module} and then sets up an {@link Injector}
 * using these modules. The {@link #getService(Class)},
 * {@link #getFirstService(Class)} and {@link #getServices(Class)} methods are
 * implemented using this Injector. Thus the services returned by this TinyPlugz
 * implementation support the full range of Guice's dependency injection
 * features including scopes, constructor injection and so on.
 *
 * @author Simon Taddiken
 */
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

    /**
     * Name for injecting the plugin ClassLoader. Just annotate a
     * field/parameter with {@code @Named(TinyPlugzGuice.PLUGIN_CLASSLOADER)}.
     * The value is guaranteed to be {@value #PLUGIN_CLASSLOADER} and will not
     * change in further versions.
     */
    public static final String PLUGIN_CLASSLOADER = "pluginClassLoader";

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
                bind(ClassLoader.class).annotatedWith(Names.named(PLUGIN_CLASSLOADER))
                        .toInstance(TinyPlugzGuice.this.pluginClassLoader);
            }
        };
        return Collections.singleton(internal);
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
        return (Iterable<Module>) props.getOrDefault(ADDITIONAL_MODULES,
                Collections.emptyList());
    }

    private Iterable<Module> getPluginModules() {
        // using the plugin class loader allows to access Services from plugins
        final Iterator<Module> moduleIt = ServiceLoader
                .load(Module.class, this.pluginClassLoader)
                .iterator();

        return Iterators.iterableOf(moduleIt);
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

    /**
     * Gets all services of the given type which have been bound during
     * initialization. The concrete behavior of this method is as follows:
     * <ol>
     * <li>If there is a multi binding for the given type, an iterator over the
     * bound services is returned.</li>
     * <li>If there is no multi binding, we try to resolve a single binding for
     * the given type. If present, an iterator over the single service is
     * returned.</li>
     * <li>If there is neither a multi binding nor a single binding for the
     * given type, an empty iterator is returned.</li>
     * </ol>
     */
    @Override
    public final <T> Iterator<T> getServices(Class<T> type) {
        Require.nonNull(type, "type");

        Iterator<T> result = null;
        try {
            final TypeLiteral<Set<T>> lit = setOf(type);
            final Key<Set<T>> key = Key.get(lit);
            final Set<T> bindings = this.injector.getInstance(key);

            result = bindings.iterator();
        } catch (ConfigurationException e) {
            LOG.warn("Could not get set bindings for '{}'", type.getName(), e);
            try {
                final T single = this.injector.getInstance(type);
                result = Iterators.singleIterator(single);
            } catch (ConfigurationException e1) {
                LOG.warn("Could not get instance for '{}'", type.getName(), e1);
                result = Collections.emptyIterator();
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> TypeLiteral<Set<T>> setOf(Class<T> type) {
        return (TypeLiteral<Set<T>>) TypeLiteral.get(Types.setOf(type));
    }

    /**
     * Returns the first service of given type. This method is implemented in
     * terms of {@link #getServices(Class)} and thus follows its described
     * behavior.
     *
     * @see #getServices(Class)
     */
    @Override
    public final <T> Optional<T> getFirstService(Class<T> type) {
        return defaultGetFirstService(type);
    }

    /**
     * Returns the single service of the given type. This method is implemented
     * in terms of {@link #getServices(Class)} and thus follows its described
     * behavior. If there is more than one or no binding for the given type, an
     * exception will be thrown.
     */
    @Override
    public final <T> T getService(Class<T> type) {
        return defaultGetService(type);
    }
}
