package de.skuzzle.tinyplugz;

import java.net.URL;
import java.util.jar.Manifest;

/**
 * Holds meta information about a single loaded plugin.
 *
 * @author Simon Taddiken
 * @since 0.2.0
 */
public interface PluginInformation {

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
