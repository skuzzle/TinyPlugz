package de.skuzzle.tinyplugz;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

final class TinyPlugzImpl extends TinyPlugz {

    private ClassLoader pluginClassLoader;

    @Override
    protected final void initializeInstance(Set<URL> urls,
            ClassLoader applicationClassLoader) {
        this.pluginClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]),
                applicationClassLoader);
    }

    @Override
    public final ClassLoader getClassLoader() {
        return this.pluginClassLoader;
    }

    @Override
    protected final void runMain(String className, String[] args)
            throws TinyPlugzException {
        try {
            Thread.currentThread().setContextClassLoader(this.pluginClassLoader);
            final Class<?> cls = this.pluginClassLoader.loadClass(className);
            final Method method = cls.getMethod("main", new Class<?>[] { String[].class });

            boolean bValidModifiers = false;
            boolean bValidVoid = false;

            if (method != null) {
                method.setAccessible(true); // Disable IllegalAccessException
                int nModifiers = method.getModifiers(); // main() must be
                                                        // "public static"
                bValidModifiers = Modifier.isPublic(nModifiers) &&
                    Modifier.isStatic(nModifiers);
                Class<?> clazzRet = method.getReturnType(); // main() must be
                                                            // "void"
                bValidVoid = (clazzRet == void.class);
            }
            if (method == null || !bValidModifiers || !bValidVoid) {
                throw new TinyPlugzException(
                        "The main() method in class \"" + cls.getName() + "\" not found.");
            }

            // Invoke method.
            // Crazy cast "(Object)args" because param is: "Object... args"
            method.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw new TinyPlugzException(e.getTargetException());
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | ClassNotFoundException e) {
            throw new TinyPlugzException(e);
        }
    }

    @Override
    public Optional<URL> getResource(String name) {
        return Optional.ofNullable(this.pluginClassLoader.getResource(name));
    }

    @Override
    public Iterator<URL> getResources(String name) throws IOException {
        final Enumeration<URL> e = this.pluginClassLoader.getResources(name);
        return new Iterator<URL>() {

            @Override
            public boolean hasNext() {
                return e.hasMoreElements();
            }

            @Override
            public URL next() {
                return e.nextElement();
            }
        };
    }

    @Override
    public final void contextClassLoaderScope(Runnable r) {
        final Thread current = Thread.currentThread();
        final ClassLoader contextCl = current.getContextClassLoader();
        try {
            current.setContextClassLoader(this.pluginClassLoader);
            r.run();
        } finally {
            current.setContextClassLoader(contextCl);
        }
    }

    @Override
    public final <T> Iterator<T> loadServices(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }

        return ServiceLoader.load(type, this.pluginClassLoader).iterator();
    }

    @Override
    public final <T> Optional<T> loadFirstService(Class<T> type) {
        final Iterator<T> services = loadServices(type);
        return services.hasNext()
                ? Optional.of(services.next())
                : Optional.empty();
    }

    @Override
    public final <T> T loadService(Class<T> type) {
        final Iterator<T> services = loadServices(type);
        if (!services.hasNext()) {
            throw new IllegalStateException(String.format(
                    "no provider for service '%s' found", type));
        }
        final T first = services.next();
        if (services.hasNext()) {
            throw new IllegalStateException(String.format(
                    "there are multiple providers for the service '%s'", type));
        }

        return first;
    }

}
