package de.skuzzle.tinyplugz;

import java.net.URL;
import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Builder for configuring the plugins to be deployed by {@link TinyPlugz}. This
 * interface is not intended to be implemented by clients. An instance of this
 * interface can be consumed while configuring the TinyPlugz instance before
 * deployment using the {@link TinyPlugzConfigurator}.
 *
 * @author Simon Taddiken
 */
public interface PluginSourceBuilder {

    /**
     * No-op PluginSourceBuilder consumer method for specifying no plugins when
     * configuring a TinyPlugz instance. This is required because specifying
     * plugins is the last step in the configurator's fluent builder API. So if
     * you do not want to load any plugins, you might configure your TinyPlugz
     * like this:
     *
     * <pre>
     * TinyPlugzConfigurator.setup().withPlugins(PluginSourceBuilder::noPlugins).deploy();
     * </pre>
     *
     * @param source The plugin source.
     */
    public static void noPlugins(PluginSourceBuilder source) {
        // do nothing
    }

    /**
     * Includes all plugins from the given source in the source which is to be
     * built by this builder. The URLS from the source's stream will be eagerly
     * processed by this method.
     *
     * @param source The source to include.
     * @return This instance.
     */
    PluginSourceBuilder include(PluginSource source);

    /**
     * Adds a plugin which is located by the given URL.
     *
     * @param url The URL of the plugin.
     * @return This instance.
     * @since 0.2.0
     */
    PluginSourceBuilder addPlugin(URL url);

    /**
     * Adds a plugin which is not packed into a jar but which contents are
     * contained in the given folder.
     *
     * @param folder The folder.
     * @return This instance.
     * @throws IllegalArgumentException If the given path does not denote a
     *             folder.
     */
    PluginSourceBuilder addUnpackedPlugin(Path folder);

    /**
     * Adds the given jar file as plugin.
     *
     * @param jarFile Path to the jar file.
     * @return This instance.
     */
    PluginSourceBuilder addPluginJar(Path jarFile);

    /**
     * Adds all jar files from the given folder as plugin. The search is not
     * done recursively in sub folders.
     *
     * @param folder The folder containing the files.
     * @return This instance.
     * @throws IllegalArgumentException If the given path does not denote a
     *             folder.
     */
    default PluginSourceBuilder addAllPluginJars(Path folder) {
        return addAllPluginJars(folder, path -> true);
    }

    /**
     * Adds all jar files from the given folder for which the given predicate
     * holds true as plugin. The search is not done recursively in sub folders.
     *
     * @param folder The folder containing the files.
     * @param filter The filter.
     * @return This instance.
     * @throws IllegalArgumentException If the given path does not denote a
     *             folder.
     */
    PluginSourceBuilder addAllPluginJars(Path folder, Predicate<Path> filter);

    /**
     * Creates a {@link PluginSource} from the configured URLs.
     *
     * @return The new PluginSource.
     */
    PluginSource createSource();
}
