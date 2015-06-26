package de.skuzzle.tinyplugz.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.tinyplugz.PluginInformation;
import de.skuzzle.tinyplugz.util.Closeables;
import de.skuzzle.tinyplugz.util.ElementIterator;
import de.skuzzle.tinyplugz.util.Require;

/**
 * ClassLoader for loading classes from a single plugin, given as URL. This
 * ClassLoader will create a child ClassLoader for accessing dependencies of the
 * plugin. Dependencies must be stated in the plugin's manifest Class-Path
 * attribute. Each entry will be interpreted relative to the plugin's base path.
 *
 * @author Simon Taddiken
 */
final class PluginClassLoader extends URLClassLoader implements DependencyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PluginClassLoader.class);

    /** Holds a lock object per class name shared among all plugin ClassLoaders. */
    private static final Map<String, Object> STATIC_LOCK_MAP = new HashMap<>();

    /**
     * Some static manifest file names in case the underlying file system is
     * case sensitive.
     */
    private static final String[] MANIFEST_NAMES = {
            "MANIFEST.MF",
            "MANIFEST.mf",
            "manifest.mf",
            "manifest.MF"
    };

    /** To split classpath entries. */
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    /** URL to the plugin this loader belongs to. */
    private final URL self;

    /** Base path of the plugin, obtained from the plugin's URL. */
    private final String basePath;

    /** A simple name describing the plugin loaded by this loader */
    private final String simpleName;

    /**
     * Optionally created loader to access dependencies stated in plugin's
     * MANIFEST Class-Path entry. This field will be <code>null</code> if this
     * plugin has no dependencies.
     */
    private final URLClassLoader dependencyClassLoader;

    /** Resolver to access classes and resources from other loaded plugins. */
    private final DependencyResolver dependencyResolver;

    /** The contents of the manifest.mf of this plugin. */
    private final Manifest manifest;

    private final PluginInformation information;

    /**
     * Counts nested calls to {@link #loadClass(String, boolean)} coming from
     * other plugins.
     */
    private final ThreadLocal<Integer> foreignEnterCount;

    /** Counts all nested calls to {@link #loadClass(String, boolean)}. */
    private final ThreadLocal<Integer> localEnterCount;

    private PluginClassLoader(URL pluginUrl, ClassLoader appClassLoader,
            DependencyResolver dependencyResolver) {
        super(new URL[] { pluginUrl }, appClassLoader);

        this.foreignEnterCount = ThreadLocal.withInitial(() -> 0);
        this.localEnterCount = ThreadLocal.withInitial(() -> 0);
        this.dependencyResolver = dependencyResolver;
        this.self = pluginUrl;
        this.basePath = getBasePathOf(pluginUrl);

        this.manifest = readManifest();
        this.simpleName = getName(this.manifest, pluginUrl);
        this.dependencyClassLoader = createDependencyClassLoader(this.manifest);
        this.information = new PluginInformationImpl();
    }

    static PluginClassLoader create(URL plugin, ClassLoader appClassLoader,
            DependencyResolver dependencyResolver) {
        Require.nonNull(plugin, "plugin");
        Require.nonNull(appClassLoader, "appClassLoader");
        Require.nonNull(dependencyResolver, "dependencyResolver");

        return AccessController.doPrivileged(new PrivilegedAction<PluginClassLoader>() {

            @Override
            public PluginClassLoader run() {
                LOG.debug("Loading plugin from {}", plugin);
                return new PluginClassLoader(plugin, appClassLoader, dependencyResolver);
            }

        });
    }

    /**
     * Gets information about the plugin loaded by the ClassLoader.
     *
     * @return The plugin information.
     */
    public final PluginInformation getPluginInformation() {
        return this.information;
    }

    /**
     * Gets the base path of the plugin loaded by this Classloader. If the
     * plugin was loaded from a jar, its base path is the folder that contains
     * the jar. If it was loaded from a directory, just that directory is the
     * base path.
     * <p>
     * The path returned by this method will always end with a '/'
     *
     * @return The base path.
     */
    public final String getBasePath() {
        return this.basePath;
    }

    @Override
    protected final Object getClassLoadingLock(String className) {
        // synchronizes class loading among all plugin classloaders.
        synchronized (STATIC_LOCK_MAP) {
            Object lock = STATIC_LOCK_MAP.get(className);
            if (lock == null) {
                lock = new Object();
                STATIC_LOCK_MAP.put(className, lock);
            }
            return lock;
        }
    }

    @Override
    protected final Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {

        LOG.debug("{}.loadClass('{}')", getSimpleName(), name);

        final int localCount = this.localEnterCount.get();
        Class<?> c;
        synchronized (getClassLoadingLock(name)) {
            c = findLoadedClass(name);
            try {
                // count every nested call per thread to distinguish between
                // direct calls to this method and calls coming from other
                // plugins.
                this.localEnterCount.set(localCount + 1);

                if (c == null) {
                    try {
                        c = getParent().loadClass(name);
                        if (c.getClassLoader() == null) {
                            LOG.debug("'{}' loaded by <bootstrap classloader>", name);
                        } else {
                            LOG.debug("'{}' loaded by <{}>", name, c.getClassLoader());
                        }
                    } catch (final ClassNotFoundException ignore) {
                        // do nothing but continue search
                        LOG.trace("Class '{}' not found using parent '{}' of '{}'", name,
                                getParent(), getSimpleName(), ignore);
                    }
                }

                if (c == null) {
                    if (this.foreignEnterCount.get().equals(this.localEnterCount.get())) {
                        // load class request from foreign plugin. We only look
                        // up the requested class in our own class path without
                        // querying the other plugins
                        c = super.findClass(name);
                    } else {
                        // load class request from own plugin. We need to query
                        // the other plugin ClassLoaders too in case this is not
                        // a class from our own class path.
                        c = findClass(name);
                    }
                }
            } finally {
                this.localEnterCount.set(localCount);
            }

            if (resolve) {
                resolveClass(c);
            }

            final ClassLoader winner = c.getClassLoader() == null
                    ? this
                    : c.getClassLoader();
            LOG.debug("'{}' loaded by <{}>", name, winner);
            return c;
        }
    }

    private String getBasePathOf(URL url) {
        String path = url.getPath();
        // If the url ends with '/' it already denotes the base directory.
        // Otherwise it is a file for which we need to extract the parent folder
        if (!path.endsWith("/")) {
            // this is an URL to a file
            final int i = path.lastIndexOf('/');
            // +1 to include slash
            path = path.substring(0, i + 1);
        }
        return path;
    }

    private String getName(Manifest mf, URL url) {
        final String mfName = mf.getMainAttributes().getValue(Name.IMPLEMENTATION_TITLE);
        if (mfName != null) {
            return mfName;
        }

        // backup: extract from file name
        String path = url.getPath();
        int j = -1;
        if (path.endsWith("/")) {
            // strip off trailing /
            path = path.substring(0, path.length() - 1);
        } else {
            // if it's a file, we strip off the extension
            j = path.lastIndexOf('.');
        }

        j = j == -1
                ? path.length()
                : j;
        final int i = path.lastIndexOf('/');
        path = path.substring(i + 1, j);
        return path;
    }

    private Manifest readManifest() {
        final URL mfURL = findManifestUrl();
        Manifest result = new Manifest();
        if (mfURL != null) {
            try (InputStream in = mfURL.openStream()) {
                result = new Manifest(in);
            } catch (final IOException e) {
                LOG.error("Error reading manifest file for {}", this.self, e);
            }
        } else {
            LOG.trace("Plugin '{}' has no manifest", getSimpleName());
        }
        return result;
    }

    private DependencyClassLoader createDependencyClassLoader(Manifest mf) {
        final DependencyClassLoader result;
        final String cp = mf.getMainAttributes().getValue(Name.CLASS_PATH);
        if (cp == null) {
            LOG.trace("Plugin '{}' has no Class-Path attribute", getSimpleName());
            result = null;
        } else {
            final String[] entries = WHITESPACES.split(cp);
            result = fromClassPath(entries);
        }

        return result;
    }

    private DependencyClassLoader fromClassPath(String[] entries) {
        final URL[] urls = Arrays.stream(entries)
                .map(this::resolveRelative)
                .filter(url -> url != null)
                .peek(url ->
                        LOG.debug("Add dependency of <{}>: '{}'", getSimpleName(), url)
                )
                .toArray(size -> new URL[size]);

        if (urls.length > 0) {
            return AccessController.doPrivileged(
                    new PrivilegedAction<DependencyClassLoader>() {

                        // Dependency classloader gets the same parent as the
                        // plugin loader -> dependencies can not load classes
                        // from plugins
                        @Override
                        public DependencyClassLoader run() {
                            return new DependencyClassLoader(urls,
                                    PluginClassLoader.this.getParent());
                        }
                    });
        }
        return null;
    }

    private URL resolveRelative(String name) {
        try {
            return new URL(this.self.getProtocol(), this.self.getHost(),
                    this.self.getPort(), this.basePath + name.trim());
        } catch (final MalformedURLException e) {
            LOG.error("Error constructing relative url with base path '{}' and name '{}'",
                    this.basePath, name, e);
        }
        return null;
    }

    private URL findManifestUrl() {
        URL url = null;
        int i = 0;
        do {
            // crucial to use super method because we only want to search our
            // own jar
            url = super.findResource("META-INF/" + MANIFEST_NAMES[i]);
            ++i;
        } while (url == null && i < MANIFEST_NAMES.length);
        return url;
    }

    @Override
    public final String getSimpleName() {
        return this.simpleName;
    }

    @Override
    protected final Class<?> findClass(String name) throws ClassNotFoundException {
        final Class<?> cls = findClass(this, name);
        if (cls == null) {
            throw new ClassNotFoundException(name);
        }
        return cls;
    }

    @Override
    public final URL findResource(String name) {
        return findResource(this, name);
    }

    @Override
    public final Enumeration<URL> findResources(String name) throws IOException {
        final Collection<URL> urls = new ArrayList<>();
        findResources(this, name, urls);
        return ElementIterator.wrap(urls.iterator());
    }

    private Class<?> loadClassForForeignPlugin(String name)
            throws ClassNotFoundException {
        // when searching a class for a foreign plugin, it must
        // be returned by 'loadClass' in order for this
        // classloader to get registered as the 'defining
        // classloader' for that class.
        final int count = this.foreignEnterCount.get();
        try {
            this.foreignEnterCount.set(count + 1);
            return loadClass(name);
        } finally {
            this.foreignEnterCount.set(count);
        }
    }

    @Override
    public final Class<?> findClass(@Nullable DependencyResolver requestor, String name) {
        Require.nonNull(name, "name");

        LOG.debug("{}.findClassFor(<{}>, '{}')", getSimpleName(), nameOf(requestor),
                name);

        Class<?> result;
        synchronized (getClassLoadingLock(name)) {
            // first, look up in own jar
            result = findLoadedClass(name);
        }

        if (result == null) {
            try {
                if (equals(requestor)) {
                    // request from own plugin
                    // INVARIANT: we have a lock on getClassLoadingLock()
                    result = super.findClass(name);
                } else {
                    result = loadClassForForeignPlugin(name);
                }
            } catch (final ClassNotFoundException ignore) {
                // ignore and continue search
                LOG.trace("Class '{}' not found in '{}' (request by '{}')", name,
                        getSimpleName(), nameOf(requestor), ignore);
            }
        } else if (result.getClassLoader().equals(this.dependencyClassLoader)
            && !equals(requestor)) {
            // the class has already been loaded but it is not visible for
            // the requestor because it has been loaded by the dependency
            // loader.
            result = null;
        }

        // second, look up in our dependencies
        if (result == null && equals(requestor)) {
            // INVARIANT: we have a lock on getClassLoadingLock()

            if (this.dependencyClassLoader != null) {
                try {
                    result = this.dependencyClassLoader.loadClass(name);
                } catch (final ClassNotFoundException ignore) {
                    // ignore and continue
                    LOG.trace("Class '{}' not found as dependency of '{}'", name,
                            getSimpleName(), ignore);
                }
            }

            // third, look up in other plugins
            if (result == null) {
                // It is only allowed that one plugin at a time searches for
                // classes within other plugins. Otherwise we would allow
                // deadlock conditions
                result = this.dependencyResolver.findClass(requestor, name);
            }
        }
        return result;
    }

    @Override
    public URL findResource(DependencyResolver requestor, String name) {
        Require.nonNull(name, "name");
        LOG.debug("{}.findResourceFor(<{}>, '{}')", getSimpleName(),
                nameOf(requestor), name);

        // look up in own jar
        URL url = super.findResource(name);

        if (url == null && equals(requestor)) {
            // second look up in our dependencies
            if (this.dependencyClassLoader != null) {
                url = this.dependencyClassLoader.findResource(name);
            }

            // third, look up in other plugins
            if (url == null) {
                url = this.dependencyResolver.findResource(requestor, name);
            }
        }
        return url;
    }

    @Override
    public void findResources(DependencyResolver requestor, String name,
            Collection<URL> target) throws IOException {
        Require.nonNull(name, "name");
        LOG.debug("{}.findResourcesFor(<{}>, '{}')", getSimpleName(),
                nameOf(requestor), name);

        // look up in own jar
        final Enumeration<URL> selfResult = super.findResources(name);
        addAll(target, selfResult);

        if (equals(requestor)) {

            // look up in dependencies
            if (this.dependencyClassLoader != null) {
                final Enumeration<URL> dependencyResult =
                        this.dependencyClassLoader.findResources(name);

                addAll(target, dependencyResult);
            }

            // look up in other plugins
            this.dependencyResolver.findResources(requestor, name, target);
        }
    }

    private <T> void addAll(Collection<T> target, Enumeration<T> elements) {
        while (elements.hasMoreElements()) {
            target.add(elements.nextElement());
        }
    }

    @Override
    public final String toString() {
        return "PluginClassLoader[" + this.simpleName + "]";
    }

    private String nameOf(DependencyResolver requestor) {
        return requestor == null
                ? "application"
                : requestor.getSimpleName();
    }

    @Override
    public final void close() throws IOException {
        final boolean success = Closeables.safeCloseAll(super::close,
                this.dependencyClassLoader);
        if (!success) {
            throw new IOException(String.format("Error while closing %s", this));
        }
    }

    /**
     * URLClassLoader extension just to recognize the plugin it loads the
     * classes for.
     *
     * @author Simon Taddiken
     */
    final class DependencyClassLoader extends URLClassLoader {

        private DependencyClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        final String getPluginName() {
            return getSimpleName();
        }

        @Override
        public String toString() {
            return "[DependencyClassLoader of " + getSimpleName() + "]";
        }

    }

    private final class PluginInformationImpl implements PluginInformation {

        @Override
        public final URL getLocation() {
            return PluginClassLoader.this.self;
        }

        @Override
        public final ClassLoader getClassLoader() {
            return PluginClassLoader.this;
        }

        @Override
        public final Manifest getManifest() {
            return PluginClassLoader.this.manifest;
        }

        @Override
        public final String toString() {
            return getSimpleName();
        }

        @Override
        public final int hashCode() {
            return PluginClassLoader.this.self.toString().hashCode();
        }

        @Override
        public final boolean equals(Object obj) {
            return obj == this || obj instanceof PluginInformation &&
                (((PluginInformation) obj).getLocation().toString())
                        .equals(getLocation().toString());
        }
    }
}
