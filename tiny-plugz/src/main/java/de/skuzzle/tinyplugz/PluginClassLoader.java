package de.skuzzle.tinyplugz;

import java.io.File;
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
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private PluginClassLoader(URL pluginUrl, ClassLoader appClassLoader,
            DependencyResolver dependencyResolver) {
        super(new URL[] { pluginUrl }, appClassLoader);

        this.dependencyResolver = dependencyResolver;
        this.self = pluginUrl;
        this.basePath = getBasePathOf(pluginUrl);
        this.simpleName = getName(pluginUrl);
        this.dependencyClassLoader = createDependencyClassLoader();
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
     * Gets the URL pointing to the location from which this plugin was loaded.
     *
     * @return The plugin's location.
     */
    public final URL getPluginURL() {
        return this.self;
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
    protected final Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {

        final Class<?> c = super.loadClass(name, resolve);
        final ClassLoader loader = c.getClassLoader() == null
                ? this
                : c.getClassLoader();
        LOG.debug("{} loaded by {}", name, loader);
        return c;
    }

    private String getBasePathOf(URL url) {
        String path = url.getPath();
        // If the url ends with '/' it already denotes the base directory.
        // Otherwise it is a file for which we need to extract the parent folder
        if (!path.endsWith("/")) {
            // this is an URL to a file
            int i = path.lastIndexOf('/');
            path = path.substring(0, i + 1); // +1 to include slash
        }
        return path;
    }

    private String getName(URL url) {
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
        int i = path.lastIndexOf('/');
        path = path.substring(i + 1, j);
        return path;
    }

    private URLClassLoader createDependencyClassLoader() {
        final URL mfURL = findManfestUrl();
        if (mfURL == null) {
            LOG.trace("Plugin '{}' has no manifest", getSimpleName());
            return null;
        }
        try (InputStream in = mfURL.openStream()) {
            final Manifest mf = new Manifest(in);
            final String cp = mf.getMainAttributes().getValue(Name.CLASS_PATH);
            if (cp == null) {
                LOG.trace("Plugin '{}' has no Class-Path attribute", getSimpleName());
                return null;
            }
            final String[] entries = WHITESPACES.split(cp);

            final URL[] urls = Arrays.stream(entries)
                    .map(this::resolveRelative)
                    .filter(url -> url != null)
                    .toArray(size -> new URL[size]);

            if (urls.length > 0) {
                return AccessController.doPrivileged(
                        new PrivilegedAction<URLClassLoader>() {

                            // Dependency classloader gets the same parent as
                            // the plugin loader -> dependencies can not load
                            // classes from plugins
                            @Override
                            public URLClassLoader run() {
                                return new DependencyClassLoader(urls,
                                        PluginClassLoader.this.getParent());
                            }
                        });
            }

        } catch (IOException e) {
            LOG.error("Error reading manifest file for {}", this.self, e);
        }
        return null;
    }

    private URL resolveRelative(String name) {
        try {
            final URL url = new URL(this.self.getProtocol(), this.self.getHost(),
                    this.self.getPort(), this.basePath + name.trim());
            LOG.debug("Add dependency of <{}>: '{}'", getSimpleName(), url);
            return url;
        } catch (MalformedURLException e) {
            LOG.error("Error constructing relative url with base path '{}' and name '{}'",
                    this.basePath, name, e);
        }
        return null;
    }

    private URL findManfestUrl() {
        // crucial to use super method because we only want to search our own
        // jar
        return super.findResource("META-INF/manifest.mf");
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

    @Override
    protected String findLibrary(String libname) {
        return findNativeLibrary(this, libname);
    }

    @Override
    public final Class<?> findClass(DependencyResolver requestor, String name) {
        Require.nonNull(name, "name");

        LOG.trace("{}.findClassFor(<{}>, '{}')", getSimpleName(), nameOf(requestor),
                name);
        synchronized (getClassLoadingLock(name)) {
            // first, look up in own jar
            Class<?> result = findLoadedClass(name);
            if (result == null) {
                try {
                    result = super.findClass(name);
                } catch (ClassNotFoundException ignore) {
                }
            }

            // second, look up in our dependencies
            if (result == null && equals(requestor)) {

                if (this.dependencyClassLoader != null) {
                    try {
                        result = this.dependencyClassLoader.loadClass(name);
                    } catch (ClassNotFoundException ignore) {
                    }
                }

                // third, look up in other plugins
                if (result == null) {
                    result = this.dependencyResolver.findClass(requestor, name);
                }
            }
            return result;

        }
    }

    @Override
    public URL findResource(DependencyResolver requestor, String name) {
        Require.nonNull(name, "name");
        LOG.trace("{}.findResourceFor(<{}>, '{}')", getSimpleName(),
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
        LOG.trace("{}.findResourcesFor(<{}>, '{}')", getSimpleName(),
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

    @Override
    public String findNativeLibrary(DependencyResolver requestor, String name) {
        final File path = new File(this.basePath, name);
        if (!path.exists()) {
            return null;
        }
        return path.getAbsolutePath();
    }

    private <T> void addAll(Collection<T> target, Enumeration<T> elements) {
        while (elements.hasMoreElements()) {
            target.add(elements.nextElement());
        }
    }

    @Override
    public final String toString() {
        return "[PluginClassLoader: " + this.simpleName + "]";
    }

    private String nameOf(DependencyResolver requestor) {
        return requestor == null
                ? "application"
                : requestor.getSimpleName();
    }

    @Override
    public final void close() throws IOException {
        super.close();
        if (this.dependencyClassLoader != null) {
            this.dependencyClassLoader.close();
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

        public final String getPluginName() {
            return getSimpleName();
        }
    }
}
