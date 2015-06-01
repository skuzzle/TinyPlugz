package de.skuzzle.tinyplugz;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

/**
 * Used to connect class- and resource loading between different plugins.
 *
 * @author Simon Taddiken
 */
interface DependencyResolver extends Closeable {

    /**
     * Searches for a class with given name.
     *
     * @param requestor The plugin for which the Class should be searched.
     *            Parameter will be <code>null</code> if this method is not
     *            called from any plugin (but from the application itself).
     * @param name The name of the class to search for. Must not be
     *            <code>null</code>.
     * @return The class or <code>null</code> if none was found.
     */
    public Class<?> findClass(DependencyResolver requestor, String name);

    /**
     * Searches for a resource with given name.
     *
     * @param requestor The plugin for which the resource should be searched.
     *            Parameter will be <code>null</code> if this method is not
     *            called from any plugin (but from the application itself).
     * @param name The name of the resource to search for.
     * @return An URL to the resource or <code>null</code> if none was found.
     */
    public URL findResource(DependencyResolver requestor, String name);

    /**
     * Searches for all resources with given name and collects them in the given
     * collection.
     *
     * @param requestor The plugin for which the resources should be collected.
     *            Parameter will be <code>null</code> if this method is not
     *            called from any plugin (but from the application itself).
     * @param name The name of the resource. Must not be <code>null</code>.
     * @param target Target collection.
     * @throws IOException If an IO error occurs.
     */
    public void findResources(DependencyResolver requestor, String name,
            Collection<URL> target) throws IOException;

    /**
     * Returns the path of the native library with given name.
     *
     * @param requestor The plugin that wishes to find the library. Parameter
     *            will be <code>null</code> if this method is not called from
     *            any plugin (but from the application itself).
     * @param name The name of the library. Must not be <code>null</code>.
     * @return The path to the library with given name.
     */
    public String findNativeLibrary(DependencyResolver requestor, String name);
}
