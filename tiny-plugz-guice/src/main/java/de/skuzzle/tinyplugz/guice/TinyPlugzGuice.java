package de.skuzzle.tinyplugz.guice;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.jar.Attributes.Name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import de.skuzzle.tinyplugz.DeployListener;
import de.skuzzle.tinyplugz.Options;
import de.skuzzle.tinyplugz.PluginInformation;
import de.skuzzle.tinyplugz.PluginSource;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator;
import de.skuzzle.tinyplugz.internal.DelegateClassLoader;
import de.skuzzle.tinyplugz.internal.ServiceLoaderWrapper;
import de.skuzzle.tinyplugz.util.ElementIterator;
import de.skuzzle.tinyplugz.util.Iterators;
import de.skuzzle.tinyplugz.util.ReflectionUtil;
import de.skuzzle.tinyplugz.util.Require;

/**
 * TinyPlugz implementation building upon google Guice and its Multibinding
 * extension. When {@link #initialize(PluginSource, ClassLoader, Map)
 * initialize} is called, this implementation asks the {@link ServiceLoader} for
 * providers implementing Guice's {@link Module} and then sets up an
 * {@link Injector} using these modules. Thereby modules from the host and from
 * all available plugins are collected automatically. The
 * {@link #getService(Class)}, {@link #getFirstService(Class)} and
 * {@link #getServices(Class)} methods are implemented using the created
 * Injector. Thus the services returned by this TinyPlugz implementation support
 * the full range of Guice's dependency injection features including scopes,
 * constructor injection and so on.
 *
 * <h2>Usage</h2>
 * <p>
 * This extension comes with a TinyPlugz service provider definition which will
 * automatically be recognized by the {@link TinyPlugzConfigurator} if it is the
 * only one provider on the classpath. In your host application you can still
 * force usage of this implementation by adding the
 * {@link Options#FORCE_IMPLEMENTATION} property with the value
 * {@code "de.skuzzle.tinyplugz.guice.TinyPlugzGuice"}.
 * <p>
 * <b>Note:</b> Like directly calling the Injector's
 * {@link Injector#getInstance(Class) getInstance} method, calling
 * {@link #getService(Class) getService}, {@link #getFirstService(Class)
 * getFirstService} and {@link #getServices(Class) getServices} is discouraged
 * in favor of directly injecting dependencies where needed. Please refer to the
 * Guice documentation for learning about best practices.
 *
 * <h2>Setup</h2>
 * <p>
 * The setup routine for this implementation upon deployment involves three
 * major steps which are executed automatically:
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
 * <p>
 * The setup is done in {@link #initialize(PluginSource, ClassLoader, Map)} and
 * thus during deploy-time of TinyPlugz. Please note that you can not access
 * TinyPlugz using {@link TinyPlugz#getInstance()} within your modules. However
 * you can inject the TinyPlugz instance where ever needed (e.g. as a parameter
 * to a provider method in your module). See <em>Default Bindings</em> below.
 * <p>
 * Please note that all modules which will be pulled in from the service loader
 * need a public no-argument constructor. If this is not sufficient for your
 * use-case, have a look at the {@link #ADDITIONAL_MODULES} option.
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
 *
 * <h2>Default Bindings</h2>
 * <p>
 * When creating the Injector, several default bindings are added:
 * <ol>
 * <li>A binding of {@code TinyPlugz.class} to the current
 * {@link TinyPlugzGuice} instance. So you do not need to access it using
 * {@link #getInstance()}.</li>
 * <li>A binding of {@code ClassLoader.class} named {@value #PLUGIN_CLASSLOADER}
 * to the current plugin Classloader.</li>
 * <li>For each loaded plugin, its {@link PluginInformation} instance is bound
 * with the plugin's implementation title. That title is obtained from the
 * plugin's manifest. If the manifest of a plugin specifies no title, it is left
 * out.</li>
 * <li>A method interceptor is bound which allows methods annotated with
 * {@link TinyPlugzContext} to be executed with exchanging the thread's context
 * Classloader for the TinyPlugz Classloader.</li>
 * </ol>
 *
 * <h2>Automatically Create Services</h2>
 * <p>
 * If you want to automatically create service provider bindings in the META-INF
 * directory of your application, it is recommended to use a library like
 * google's <a
 * href="https://github.com/google/auto/tree/master/service">auto-service</a>.
 * You can then implement your TinyPlugz compatible guice modules like:
 *
 * <pre>
 * package com.your.domain;
 *
 * import com.google.auto.service.AutoService;
 * import com.google.inject.AbstractModule;
 * import com.google.inject.Module;
 *
 * &#064;AutoService(Module.class)
 * public class MyModule extends AbstractModule {
 *     &#064;Override
 *     public void setup() {
 *         // ....
 *     }
 * }
 * </pre>
 *
 * The compiler will automatically create the file
 * {@code META-INF/services/com.google.inject.Module} and list the full
 * qualified names of all your modules annotated like the above.
 *
 * @author Simon Taddiken
 */
public final class TinyPlugzGuice extends TinyPlugz {

    /**
     * Initialization property to specify additional modules to be used when
     * creating the injector. You man specify any object of type
     * {@code Iterable<Module>} as value for this property.
     * <p>
     * Please note that modules which are registered as services in the host
     * application are loaded automatically and need not to be registered
     * explicitly.
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
     * {@link #PARENT_INJECTOR} will be ignored. This property supports three
     * different kind of values:
     * <ul>
     * <li>If the value is an instance of {@link InjectorFactory}, then just
     * that instance will be used.</li>
     * <li>If the value is an instance of {@link Class}, then the class's
     * default constructor will be invoked to create an {@link InjectorFactory}
     * instance.</li>
     * <li>If the value is a String, that String will be interpreted as a full
     * qualified name to class which implements {@link InjectorFactory}. The
     * class will be loaded by the parent Classloader and constructed via its
     * default constructor.</li>
     * </ul>
     */
    public static final String INJECTOR_FACTORY = "tinyplugz.guice.injectorFactory";

    /**
     * If this property is supplied during setup, the iterator returned by
     * {@link #getServices(Class)} will lazily create the services while the
     * iterator is being consumed. Any non-null value will enable this property.
     *
     * @since 0.3.0
     */
    public static final String LAZY_SERVICES = "tinyplugz.guice.lazyServices";

    /**
     * The name of the injector {@link Stage} to use. Only applicable when using
     * the default {@link InjectorFactory} implementation.
     *
     * @since 0.4.0
     */
    public static final String INJECTOR_STAGE = "tinyplugz.guice.injectorStage";

    /**
     * Name for injecting the plugin ClassLoader. Just annotate a
     * field/parameter with {@code @Named(TinyPlugzGuice.PLUGIN_CLASSLOADER)}.
     * The value is guaranteed to be {@value #PLUGIN_CLASSLOADER} and will not
     * change in further versions.
     */
    public static final String PLUGIN_CLASSLOADER = "pluginClassLoader";

    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugzGuice.class);

    private Injector injector;
    private GetServicesStrategy getServiceStrategy;
    private DelegateClassLoader pluginClassLoader;
    private ServiceLoaderWrapper serviceLoader;

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
    protected final void initialize(PluginSource source,
            ClassLoader parentClassLoader,
            Map<Object, Object> properties) {

        this.getServiceStrategy = getGetServiceStrategy(properties);
        LOG.debug("Service strategy: {}", this.getServiceStrategy);

        this.pluginClassLoader = createClassLoader(source, parentClassLoader);

        if (properties.containsKey(Options.SERVICE_LOADER_WRAPPER)) {
            this.serviceLoader = ReflectionUtil.createInstance(
                    properties.get(Options.SERVICE_LOADER_WRAPPER),
                    ServiceLoaderWrapper.class,
                    parentClassLoader);
        } else {
            this.serviceLoader = ServiceLoaderWrapper.getDefault();
        }

        final Iterable<Module> appModules = getAdditionalModules(properties);
        final Iterable<Module> pluginModules = getPluginModules();
        final Iterable<Module> internal = getInternalModule();
        final Iterable<Module> modules = Iterators.composite(internal, appModules,
                pluginModules);
        this.injector = createInjector(properties, modules);
    }

    private GetServicesStrategy getGetServiceStrategy(Map<Object, Object> props) {
        if (props.containsKey(LAZY_SERVICES)) {
            return GetServicesStrategy.LAZY;
        }
        return GetServicesStrategy.EAGER;
    }

    @Override
    public final Collection<PluginInformation> getPluginInformation() {
        return this.pluginClassLoader.getInformation();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation post-processes each listener before returning it to
     * inject its fields using this instance's injector.
     */
    @Override
    protected final Iterator<DeployListener> findDeployListeners(
            ClassLoader pluginClassLoader) {
        final Iterator<DeployListener> listeners = this.serviceLoader.loadService(
                DeployListener.class, pluginClassLoader);

        return new Iterator<DeployListener>() {

            @Override
            public boolean hasNext() {
                return listeners.hasNext();
            }

            @Override
            public DeployListener next() {
                final DeployListener listener = listeners.next();
                TinyPlugzGuice.this.injector.injectMembers(listener);
                return listener;
            }
        };
    }

    @Override
    protected final void dispose() {
        defaultDispose();
    }

    private Iterable<Module> getInternalModule() {
        // create the default bindings
        final Module internal = new AbstractModule() {

            @Override
            protected void configure() {
                bindInterceptor(Matchers.any(),
                        Matchers.annotatedWith(TinyPlugzContext.class),
                        new TinyPlugzContextInterceptor());

                bind(TinyPlugz.class).toInstance(TinyPlugzGuice.this);
                bind(ClassLoader.class).annotatedWith(Names.named(PLUGIN_CLASSLOADER))
                        .toInstance(TinyPlugzGuice.this.pluginClassLoader);

                final Collection<PluginInformation> infos =
                        TinyPlugzGuice.this.pluginClassLoader.getInformation();
                for (final PluginInformation info : infos) {
                    final String name = info
                            .getManifest()
                            .getMainAttributes()
                            .getValue(Name.IMPLEMENTATION_TITLE);

                    if (name != null) {
                        bind(PluginInformation.class)
                                .annotatedWith(Names.named(name))
                                .toInstance(info);
                    }
                }
            }
        };
        return Collections.singleton(internal);
    }

    private Injector createInjector(Map<Object, Object> props, Iterable<Module> modules) {
        final Object value = props.get(INJECTOR_FACTORY);
        final InjectorFactory factory;
        if (value == null) {
            factory = new DefaultInjectorFactory();
        } else {
            // explicitly use application class loader because there is
            // (probably?) no need to configure the factory from plugins
            final ClassLoader applicationCl = this.pluginClassLoader.getParent();
            factory = ReflectionUtil.createInstance(value, InjectorFactory.class,
                    applicationCl);
        }

        final Injector guiceInjector = factory.createInjector(modules, props);
        Require.nonNullResult(guiceInjector, "InjectorFactory.createInjector");
        return guiceInjector;
    }

    @SuppressWarnings("unchecked")
    private Iterable<Module> getAdditionalModules(Map<Object, Object> props) {
        return (Iterable<Module>) props.getOrDefault(ADDITIONAL_MODULES,
                Collections.emptyList());
    }

    private Iterable<Module> getPluginModules() {
        // using the plugin class loader allows to access Services from plugins
        final Iterator<Module> moduleIt = this.serviceLoader.loadService(
                Module.class, this.pluginClassLoader);

        // Wrap modules for logging purposes
        final Iterator<Module> wrapped = new Iterator<Module>() {

            @Override
            public boolean hasNext() {
                return moduleIt.hasNext();
            }

            @Override
            public Module next() {
                final Module module = moduleIt.next();
                LOG.debug("Installing module '{}'", module);
                return module;
            }
        };
        return Iterators.iterableOf(wrapped);
    }

    @Override
    public final void runMain(String className, String[] args) {
        Require.nonNull(className, "className");
        Require.nonNull(args, "args");
        defaultRunMain(className, args);
    }

    @Override
    public final ClassLoader getClassLoader() {
        return this.pluginClassLoader;
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
    public final <T> ElementIterator<T> getServices(Class<T> type) {
        Require.nonNull(type, "type");

        final Iterator<T> services = this.getServiceStrategy.getServices(
                this.injector, type);
        return ElementIterator.wrap(services);
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
        Require.nonNull(type, "type");
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
        Require.nonNull(type, "type");
        return defaultGetService(type);
    }
}
