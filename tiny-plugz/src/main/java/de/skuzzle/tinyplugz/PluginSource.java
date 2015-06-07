package de.skuzzle.tinyplugz;

import java.net.URL;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Represents the source of plugins which should be loaded.
 *
 * @author Simon Taddiken
 * @since 0.2.0
 */
public interface PluginSource {

    /**
     * Creates an empty PluginSource.
     *
     * @return The new empty source.
     */
    public static PluginSource empty() {
        return new PluginSource() {

            @Override
            public Stream<URL> getPluginURLs() {
                return Collections.<URL> emptySet().stream();
            }
        };
    }

    /**
     * Gets a stream of URLs pointing to plugins to be loaded by TinyPlugz.
     *
     * @return The URL stream.
     */
    public Stream<URL> getPluginURLs();
}
