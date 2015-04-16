package de.skuzzle.tinyplugz.guice;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzException;

public final class GuiceTinyPlugzImpl extends TinyPlugz {

    @Override
    protected void initializeInstance(Set<URL> urls, ClassLoader applicationClassLoader) {}

    @Override
    protected void runMain(String cls, String[] args) throws TinyPlugzException {}

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public Optional<URL> getResource(String name) {
        return null;
    }

    @Override
    public Iterator<URL> getResources(String name) throws IOException {
        return null;
    }

    @Override
    public void contextClassLoaderScope(Runnable r) {}

    @Override
    public <T> Iterator<T> loadServices(Class<T> type) {
        return null;
    }

    @Override
    public <T> Optional<T> loadFirstService(Class<T> type) {
        return null;
    }

    @Override
    public <T> T loadService(Class<T> type) {
        return null;
    }

}
