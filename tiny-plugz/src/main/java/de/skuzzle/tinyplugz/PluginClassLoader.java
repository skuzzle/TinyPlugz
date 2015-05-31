package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PluginClassLoader extends URLClassLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PluginClassLoader.class);
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");
    private final URL self;
    private final String basePath;
    private final URLClassLoader dependencyClassLoader;

    private PluginClassLoader(URL plugin, ClassLoader parent) {
        super(new URL[] {Require.nonNull(plugin, "plugin")},
                Require.nonNull(parent, "parent"));
        this.self = plugin;
        this.basePath = getBasePath(plugin);
        this.dependencyClassLoader = addManifestDependencies();
    }

    public static PluginClassLoader create(URL plugin, CommonClassLoader parent) {
        return AccessController.doPrivileged(new PrivilegedAction<PluginClassLoader>() {

            @Override
            public PluginClassLoader run() {
                return new PluginClassLoader(plugin, parent);
            }

        });
    }

    private ClassLoader getApplicationClassLoader() {
        final ClassLoader common = getParent();
        return common.getParent();
    }

    private String getBasePath(URL url) {
        final int i = url.getPath().lastIndexOf('/');
        return url.getPath().substring(0, i);
    }

    private URLClassLoader addManifestDependencies() {
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

            final URLClassLoader dependencyCl = AccessController.doPrivileged(
                    new PrivilegedAction<URLClassLoader>() {

                @Override
                public URLClassLoader run() {
                    return new URLClassLoader(urls, getApplicationClassLoader());
                }
            });

            return dependencyCl;
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
        return findResource("META-INF/manifest.mf");
    }

    @Override
    public final Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            if (this.dependencyClassLoader != null) {
                return this.dependencyClassLoader.loadClass(name);
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public final URL findResource(String name) {
        URL url = super.findResource(name);
        if (url == null && this.dependencyClassLoader != null) {
            url = this.dependencyClassLoader.findResource(name);
        }
        return url;
    }

    @Override
    public final Enumeration<URL> findResources(String name) throws IOException {
        final Enumeration<URL> urls = super.findResources(name);
        if (this.dependencyClassLoader != null) {
            final Enumeration<URL> depUrls =
                    this.dependencyClassLoader.findResources(name);

            return Iterators.composite(
                    ElementIterator.wrap(urls),
                    ElementIterator.wrap(depUrls));
        }
        return urls;
    }

    @Override
    public final void close() throws IOException {
        super.close();
        if (this.dependencyClassLoader != null) {
            this.dependencyClassLoader.close();
        }
    }
}
