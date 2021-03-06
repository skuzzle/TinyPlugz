package de.skuzzle.tinyplugz.internal;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Used to connect class- and resource loading between different plugins. This
 * interface is only used internally and not meant to be implemented or used by
 * clients.
 *
 * @author Simon Taddiken
 */
public interface DependencyResolver extends Closeable {

    /**
     * Returns a simple displayable name for this resolver.
     *
     * @return The name.
     */
    public String getSimpleName();

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
    @Nullable
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
    public void findResources(
            DependencyResolver requestor,
            String name,
            Collection<URL> target) throws IOException;
}

