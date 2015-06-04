package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

final class DelegateDependencyResolver implements DependencyResolver {

    private final Collection<DependencyResolver> children;

    DelegateDependencyResolver(Collection<DependencyResolver> children) {
        this.children = Require.nonNull(children, "children");
    }

    @Override
    public final Class<?> findClass(DependencyResolver requestor, String name) {
        Require.nonNull(name, "name");

        for (final DependencyResolver pluginCl : this.children) {
            if (pluginCl.equals(requestor)) {
                continue;
            }
            final Class<?> cls = pluginCl.findClass(requestor, name);
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }

    @Override
    public final URL findResource(DependencyResolver requestor, String name) {
        Require.nonNull(name, "name");

        for (final DependencyResolver pluginCl : this.children) {
            if (pluginCl.equals(requestor)) {
                continue;
            }
            final URL url = pluginCl.findResource(requestor, name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    @Override
    public final void findResources(DependencyResolver requestor, String name,
            Collection<URL> target) throws IOException {
        Require.nonNull(name, "name");

        for (final DependencyResolver pluginCl : this.children) {
            if (pluginCl.equals(requestor)) {
                continue;
            }
            pluginCl.findResources(requestor, name, target);
        }
    }

    @Override
    public final void close() throws IOException {
        for (final DependencyResolver pluginCl : this.children) {
            pluginCl.close();
        }
    }

    @Override
    public final String getSimpleName() {
        return "Delegator";
    }

}
