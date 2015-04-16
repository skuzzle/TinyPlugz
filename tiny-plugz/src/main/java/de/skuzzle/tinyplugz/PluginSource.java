package de.skuzzle.tinyplugz;

import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Builder for configuring the plugins to be deployed by
 * {@link TinyPlugz#deployPlugins(java.util.function.Consumer)}.
 *
 * @author Simon Taddiken
 */
public interface PluginSource {

    /**
     * Adds a plugin which is not packed into a jar but which contents are
     * contained in the given folder.
     *
     * @param folder The folder.
     * @return This instance.
     * @throws IllegalArgumentException If the given path does not denote a
     *             folder.
     */
    PluginSource addUnpackedPlugin(Path folder);

    /**
     * Adds the given jar file as plugin.
     *
     * @param jarFile Path to the jar file.
     * @return This instance.
     */
    PluginSource addPluginJar(Path jarFile);

    /**
     * Adds all jar files from the given folder as plugin. The search is not
     * done recursively in sub folders.
     *
     * @param folder The folder containing the files.
     * @return This instance.
     * @throws IllegalArgumentException If the given path does not denote a
     *             folder.
     */
    default PluginSource addAllPluginJars(Path folder) {
        return addAllPluginJars(folder, path -> true);
    }

    /**
     * Adds all jar files from the given folder for which the given predicate
     * holds true as plugin. The search is not done recursively in sub folders.
     *
     * @param folder THe folder containint the files.
     * @param filter The filter.
     * @return This instance.
     * @throws IllegalArgumentException If the given path does not denote a
     *             folder.
     */
    PluginSource addAllPluginJars(Path folder, Predicate<Path> filter);
}
