package de.skuzzle.tinyplugz;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

final class PluginSourceBuilderImpl implements PluginSource {

    private final Set<URL> pluginUrls;

    PluginSourceBuilderImpl() {
        this.pluginUrls = new HashSet<>();
    }

    public final Set<URL> getPluginUrls() {
        return this.pluginUrls;
    }

    @Override
    public final PluginSource addUnpackedPlugin(Path folder) {
        if (folder == null) {
            throw new IllegalArgumentException("folder is null");
        } else if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException(String.format(
                    "folder '%s' does not denote a directory", folder));
        }

        addPath(folder);
        return this;
    }

    @Override
    public final PluginSource addPluginJar(Path jarFile) {
        if (jarFile == null) {
            throw new IllegalArgumentException("jarFile is null");
        }

        addPath(jarFile);
        return this;
    }

    @Override
    public final PluginSource addAllPluginJars(Path folder, Predicate<Path> filter) {
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
            this.pluginUrls.add(url);
        } catch (MalformedURLException e) {
            throw new UnsupportedOperationException(String.format(
                    "could not create URL for path '%s'", path), e);
        }
    }
}
