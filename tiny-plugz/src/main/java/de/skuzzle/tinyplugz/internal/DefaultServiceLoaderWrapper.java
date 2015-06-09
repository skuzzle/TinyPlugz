package de.skuzzle.tinyplugz.internal;

import java.util.ServiceLoader;

import de.skuzzle.tinyplugz.util.ElementIterator;


final class DefaultServiceLoaderWrapper implements ServiceLoaderWrapper {

    @Override
    public <T> ElementIterator<T> loadService(Class<T> providerClass,
            ClassLoader classLoader) {
        return ElementIterator.wrap(
                ServiceLoader.load(providerClass, classLoader).iterator());
    }

}
