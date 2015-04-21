package de.skuzzle.tinyplugz;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

final class PluginSourceBuilderImpl implements PluginSource {

    // workaround for slow equals and hashCode method of URL class
    private static final class URLKey {
        private final URL url;
        private final String key;

        private URLKey(URL url) {
            this.url = url;
            this.key = url.toString();
        }

        private URL getURL() {
            return this.url;
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof URLKey &&
                this.key.equals(((URLKey) obj).key);
        }
    }

    private final Collection<URLKey> pluginUrls;

    PluginSourceBuilderImpl() {
        this.pluginUrls = new HashSet<>();
    }

    /**
     * Gets a Stream of URLs for all added plugin locations.
     *
     * @return The configured plugins.
     */
    public final Stream<URL> getPluginUrls() {
        return this.pluginUrls.stream().map(URLKey::getURL);
    }

    @Override
    public final PluginSource addUnpackedPlugin(Path folder) {
        Require.nonNull(folder, "folder");
        Require.condition(Files.isDirectory(folder),
                "path '%s' does not denote a directory", folder);

        addPath(folder);
        return this;
    }

    @Override
    public final PluginSource addPluginJar(Path jarFile) {
        Require.nonNull(jarFile, "jarFile");
        addPath(jarFile);
        return this;
    }

    @Override
    public final PluginSource addAllPluginJars(Path folder, Predicate<Path> filter) {
        Require.nonNull(folder, "folder");
        Require.nonNull(filter, "filter");
        Require.condition(Files.isDirectory(folder),
                "path '%s' does not denote a directory", folder);

        for (int i = 0; i < folder.getNameCount(); ++i) {
            final Path elem = folder.getName(i);
            if (Files.isDirectory(elem) && filter.test(elem)) {
                addPluginJar(elem);
            }
        }
        return this;
    }

    private void addPath(Path path) {
        try {
            final URL url = path.toUri().toURL();
            this.pluginUrls.add(new URLKey(url));
        } catch (MalformedURLException e) {
            throw new UnsupportedOperationException(String.format(
                    "could not create URL for path '%s'", path), e);
        }
    }
}
