package de.skuzzle.tinyplugz;

import java.net.URL;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

/**
 * Holds meta information about a single loaded plugin.
 *
 * @author Simon Taddiken
 * @since 0.2.0
 */
public interface PluginInformation {

    /**
     * Gets the plugin's name. The name is either defined by the
     * {@link Name#IMPLEMENTATION_TITLE} attribute of the plugin's jar file or
     * by the jar file's name.
     *
     * @return The plugin name.
     * @since 0.4.0
     */
    String getName();

    /**
     * The location from which the plugin has been loaded.
     *
     * @return The location.
     */
    URL getLocation();

    /**
     * The ClassLoader which loaded the plugin.
     *
     * @return The ClassLoader.
     */
    ClassLoader getClassLoader();

    /**
     * Manifest meta information from the plugin's jar.
     *
     * @return The manifest.
     */
    Manifest getManifest();
}
