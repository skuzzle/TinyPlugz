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

final class PluginClassLoader extends URLClassLoader implements DependencyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PluginClassLoader.class);

    /** To split classpath entries. */
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    /** URL to the plugin this loader belongs to. */
    private final URL self;

    /** Base path of the plugin, obtained from the plugin's URL. */
    private final String basePath;

    /**
     * Optionally created loader to access dependencies stated in plugin's
     * MANIFEST Class-Path entry.
     */
    private final URLClassLoader dependencyClassLoader;

    /** Resolver to access classes and resources from other loaded plugins. */
    private final DependencyResolver dependencyResolver;

    private PluginClassLoader(URL pluginUrl, ClassLoader appClassLoader,
            DependencyResolver dependencyResolver) {
        super(new URL[] { Require.nonNull(pluginUrl, "pluginUrl") },
                Require.nonNull(appClassLoader, "appClassLoader"));

        this.dependencyResolver = Require.nonNull(dependencyResolver,
                "dependencyResolver");

        this.self = pluginUrl;
        this.basePath = getBasePathOf(pluginUrl);
        this.dependencyClassLoader = createDependencyClassLoader();
    }

    static PluginClassLoader create(URL plugin, ClassLoader appClassLoader,
            DependencyResolver dependencyResolver) {
        return AccessController.doPrivileged(new PrivilegedAction<PluginClassLoader>() {

            @Override
            public PluginClassLoader run() {
                return new PluginClassLoader(plugin, appClassLoader, dependencyResolver);
            }

        });
    }

    private String getBasePathOf(URL url) {
        final int i = url.getPath().lastIndexOf('/');
        return url.getPath().substring(0, i);
    }

    private URLClassLoader createDependencyClassLoader() {
        final URL mfURL = findManfestUrl();
        if (mfURL == null) {
            return null;
        }
        try (InputStream in = mfURL.openStream()) {
            final Manifest mf = new Manifest(in);
            final String cp = mf.getMainAttributes().getValue(Name.CLASS_PATH);
            final String[] entries = WHITESPACES.split(cp);

            final URL[] urls = Arrays.stream(entries)
                    .map(this::getRelativeURL)
                    .filter(url -> url != null)
                    .toArray(size -> new URL[size]);

            return AccessController.doPrivileged(
                    new PrivilegedAction<URLClassLoader>() {

                        @Override
                        public URLClassLoader run() {
                            return new URLClassLoader(urls, PluginClassLoader.this.getParent());
                        }
                    });

        } catch (IOException e) {
            LOG.error("Error reading manifest file for {0}", this.self, e);
        }
        return null;
    }

    private URL getRelativeURL(String name) {
        try {
            return new URL(this.self.getProtocol(), this.self.getHost(),
                    this.self.getPort(), this.basePath + "/" + name);
        } catch (MalformedURLException e) {
            LOG.error("Error constructing relative url with base path {0} and name {1}",
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
    protected final Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(this, name);
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
        // first, look up in own jar
        Class<?> result = null;
        try {
            result = super.findClass(name);
        } catch (ClassNotFoundException ignore) {
            LOG.trace("Class {0} not found in plugin itself", name, ignore);
        }

        // second, look up in our dependencies
        if (result == null && equals(requestor)) {

            if (this.dependencyClassLoader != null) {
                try {
                    result = this.dependencyClassLoader.loadClass(name);
                } catch (ClassNotFoundException ignore) {
                    LOG.trace("Class {0} not found in dependencies", name, ignore);
                }
            }

            // third, look up in other plugins
            if (result == null) {
                result = this.dependencyResolver.findClass(requestor, name);
            }
        }
        return result;
    }

    @Override
    public URL findResource(DependencyResolver requestor, String name) {
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
        return "[PluginClassLoader: " + this.self + "]";
    }

    @Override
    public final void close() throws IOException {
        super.close();
        if (this.dependencyClassLoader != null) {
            this.dependencyClassLoader.close();
        }
    }
}
