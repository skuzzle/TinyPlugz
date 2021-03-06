package de.skuzzle.tinyplugz.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.Nullable;

import de.skuzzle.tinyplugz.PluginSource;
import de.skuzzle.tinyplugz.PluginSourceBuilder;
import de.skuzzle.tinyplugz.util.Require;

/**
 * Default implementation for {@link PluginSourceBuilder}.
 *
 * @author Simon Taddiken
 */
public final class PluginSourceBuilderImpl implements PluginSourceBuilder {

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
        public boolean equals(@Nullable Object obj) {
            return obj == this || obj instanceof URLKey &&
                this.key.equals(((URLKey) obj).key);
        }
    }

    private final Collection<URLKey> pluginUrls = new HashSet<>();

    @Override
    public PluginSourceBuilder include(@Nullable PluginSource source) {
        final PluginSource pluginSource = Require.nonNull(source, "source");
        pluginSource.getPluginURLs().forEach(this::addPlugin);
        return this;
    }

    @Override
    public final PluginSourceBuilder addUnpackedPlugin(@Nullable Path folder) {
        Require.nonNull(folder, "folder");
        Require.condition(Files.isDirectory(folder),
                "path '%s' does not denote a directory", folder);

        addPath(folder);
        return this;
    }

    @Override
    public final PluginSourceBuilder addPluginJar(@Nullable Path jarFile) {
        Require.nonNull(jarFile, "jarFile");
        addPath(jarFile);
        return this;
    }

    @Override
    public final PluginSourceBuilder addAllPluginJars(@Nullable Path folder,
            @Nullable Predicate<Path> filter) {
        Require.nonNull(folder, "folder");
        Require.nonNull(filter, "filter");
        Require.condition(Files.isDirectory(folder),
                "path '%s' does not denote a directory", folder);

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(folder)) {
            StreamSupport.stream(dirStream.spliterator(), false)
                    .filter(this::isJar)
                    .filter(filter)
                    .forEach(this::addPath);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "IO error occurred during listing of plugins", e);
        }

        return this;
    }

    private boolean isJar(Path path) {
        return !Files.isDirectory(path) &&
            path.getFileName().toString().toLowerCase().endsWith(".jar");
    }

    @Override
    public PluginSourceBuilder addPlugin(@Nullable URL url) {
        Require.nonNull(url, "url");
        this.pluginUrls.add(new URLKey(url));
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

    @Override
    public PluginSource createSource() {
        return new PluginSource() {

            @Override
            public Stream<URL> getPluginURLs() {
                return PluginSourceBuilderImpl.this.pluginUrls.stream()
                        .map(key -> key.getURL());
            }
        };
    }
}
