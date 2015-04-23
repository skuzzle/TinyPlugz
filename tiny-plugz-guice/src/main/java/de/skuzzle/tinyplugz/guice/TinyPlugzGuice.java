package de.skuzzle.tinyplugz.guice;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
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
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Types;

import de.skuzzle.tinyplugz.ContextAction;
import de.skuzzle.tinyplugz.Options;
import de.skuzzle.tinyplugz.Require;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator;
import de.skuzzle.tinyplugz.TinyPlugzException;

/**
 * TinyPlugz implementation building upon google Guice and its Multibinding
 * extension. When {@link #initialize(Collection, ClassLoader, Map) initialize}
 * is called, this implementation asks the {@link ServiceLoader} for providers
 * implementing Guice's {@link Module} and then sets up an {@link Injector}
 * using these modules. Thereby modules from the host and from all available
 * plugins are collected automatically. The {@link #getService(Class)},
 * {@link #getFirstService(Class)} and {@link #getServices(Class)} methods are
 * implemented using the created Injector. Thus the services returned by this
 * TinyPlugz implementation support the full range of Guice's dependency
 * injection features including scopes, constructor injection and so on.
 *
 * <h2>Usage</h2>
 * <p>
 * This extension comes with a TinyPlugz service provider definition which will
 * automatically be recognized by the {@link TinyPlugzConfigurator} if it is the
 * only one provider on the classpath. In your host application you can still
 * force usage of this implementation by adding the
 * {@link Options#FORCE_IMPLEMENTATION} property with the value
 * {@code "de.skuzzle.tinyplugz.guice.TinyPlugzGuice"}.
 * </p>
 *
 * <h2>Setup</h2>
 * <p>
 * The setup routine for this implementation upon deployment involves three
 * major steps.
 *
 * <ol>
 * <li>Creation of the plugin Classloader to access resouces/classes from
 * plugins.</li>
 * <li>Collecting instances of Guice {@link Module Modules} by querying the
 * {@link ServiceLoader} using {@code Module.class} as service interface and the
 * plugin Classloader as Classloader. This will obtain all Modules which were
 * registered as service provider interface in either the host application or
 * the available plugins.</li>
 * <li>Setting up the Guice Injector with the Modules from the step before and
 * if configured, additional Modules (see {@link #ADDITIONAL_MODULES}). The
 * injector will either be a stand-alone injector or a child injector of a
 * configured parent injector (see {@link #PARENT_INJECTOR}).</li>
 * </ol>
 * </p>
 * <p>
 * The setup is done in {@link #initialize(Collection, ClassLoader, Map)} and
 * thus during deploy-time of TinyPlugz. Please note that you can not access
 * TinyPlugz using {@link TinyPlugz#getDefault()} within your modules. However
 * you can inject the TinyPlugz instance where ever needed (e.g. as a parameter
 * to a provider method in your module). See <em>Default Bindings</em> below.
 * </p>
 *
 * <h2>Service Resolution</h2>
 * <p>
 * Services are obtained using three Guice idioms: multibindings, linked
 * bindings and just-in-time bindings. Given a service type {@code T}, the
 * {@link #getServices(Class)} method will first attempt to obtain a
 * {@code Set<T>} from the Injector which might have been bound using a
 * {@link Multibinder}. If there is no binding or the set is empty, the Injector
 * is queried for a single instance of type {@code T}, returning either an
 * explicitly bound instance or an instance created from a just-in-time binding.
 * If this does not yield an result either, an empty Iterator will be returned.
 * </p>
 *
 * <h2>Default Bindings</h2>
 * <p>
 * When creating the Injector, two default bindings are added:
 * <ol>
 * <li>A binding of {@code TinyPlugz.class} to the current
 * {@link TinyPlugzGuice} instance. So you do not need to access it using
 * {@link #getDefault()}.</li>
 * <li>A binding of {@code ClassLoader.class} named {@value #PLUGIN_CLASSLOADER}
 * to the current plugin Classloader.</li>
 * </ol>
 * </p>
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
     * Property for specifying a custom {@link InjectorFactory} which will be
     * used to create the Injector. If this property is present, the property
     * {@link #PARENT_INJECTOR} will be ignored.
     */
    public static final String INJECTOR_FACTORY = "tinyplugz.guice.injectorFactory";

    /**
     * Name for injecting the plugin ClassLoader. Just annotate a
     * field/parameter with {@code @Named(TinyPlugzGuice.PLUGIN_CLASSLOADER)}.
     * The value is guaranteed to be {@value #PLUGIN_CLASSLOADER} and will not
     * change in further versions.
     */
    public static final String PLUGIN_CLASSLOADER = "pluginClassLoader";

    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugzGuice.class);

    private Injector injector;
    private ClassLoader pluginClassLoader;

    /**
     * Public no argument constructor for java's ServiceLoader. For proper
     * usage, do not instantiate this class manually. See the TinyPlugz
     * documentation to learn about proper usage.
     *
     * @deprecated Do not manually instantiate this class.
     */
    @Deprecated
    public TinyPlugzGuice() {
        // ServiceLoader default constructor
    }

    @Override
    protected final void initialize(Collection<URL> urls,
            ClassLoader parentClassLoader,
            Map<Object, Object> properties) {

        this.pluginClassLoader = createClassLoader(urls, parentClassLoader);

        final Iterable<Module> appModules = getAdditionalModules(properties);
        final Iterable<Module> pluginModules = getPluginModules();
        final Iterable<Module> internal = getInternalModule();
        final Iterable<Module> modules = Iterators.composite(internal, appModules,
                pluginModules);
        this.injector = createInjector(properties, modules);
    }

    private Iterable<Module> getInternalModule() {
        // create the default bindings
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
        InjectorFactory factory = (InjectorFactory) props.get(INJECTOR_FACTORY);
        if (factory == null) {
            factory = new DefaultInjectorFactory();
        }
        final Injector injector = factory.createInjector(modules, props);
        Require.nonNullResult(injector, "InjectorFactory.createInjector");
        return injector;
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
    public final void contextClassLoaderScope(ContextAction action) {
        defaultContextClassLoaderScope(action);
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
