package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

interface DependencyResolver {

    public Class<?> findClass(PluginClassLoader request, String name);

    public void findResources(PluginClassLoader requestor, String name,
            Collection<URL> target) throws IOException;

    public URL findResource(PluginClassLoader requestor, String name);
}
