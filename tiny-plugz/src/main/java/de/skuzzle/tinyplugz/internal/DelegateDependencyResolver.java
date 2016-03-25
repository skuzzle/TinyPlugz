package de.skuzzle.tinyplugz.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.tinyplugz.util.Closeables;
import de.skuzzle.tinyplugz.util.Require;

class DelegateDependencyResolver implements DependencyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(
            DelegateDependencyResolver.class);

    private final Collection<DependencyResolver> children;
    private final Map<String, DependencyResolver> packageIndex;

    DelegateDependencyResolver(Collection<DependencyResolver> children) {
        this.children = Require.nonNull(children, "children");
        this.packageIndex = new ConcurrentHashMap<>();
    }

    private String getPackageName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            // default package o_O
            return "";
        }
        return name.substring(0, lastDot);
    }

    @Override
    public final Class<?> findClass(@Nullable DependencyResolver requestor, String name) {
        Require.nonNull(name, "name");

        // first, try package index
        final String packageName = getPackageName(name);
        final DependencyResolver indexResolver = this.packageIndex.get(packageName);
        if (indexResolver != null && !indexResolver.equals(requestor)) {
            final Class<?> indexCls = indexResolver.findClass(requestor, name);
            if (indexCls != null) {
                return indexCls;
            }
        }

        for (final DependencyResolver pluginCl : this.children) {
            // do not ask requestor nor ask the resolver from the index again
            if (pluginCl.equals(requestor) || pluginCl.equals(indexResolver)) {
                continue;
            }
            final Class<?> cls = pluginCl.findClass(requestor, name);
            if (cls != null) {
                LOG.trace("Update package index mapping: {} -> {}",
                        packageName, pluginCl);
                this.packageIndex.put(packageName, pluginCl);
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
        Closeables.close(this.children);
    }

    @Override
    public final String getSimpleName() {
        return "Delegator";
    }

}
