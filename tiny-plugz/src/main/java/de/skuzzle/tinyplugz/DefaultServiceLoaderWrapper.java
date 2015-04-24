package de.skuzzle.tinyplugz;

import java.util.Iterator;
import java.util.ServiceLoader;

final class DefaultServiceLoaderWrapper implements ServiceLoaderWrapper {

    @Override
    public <T> Iterator<T> loadService(Class<T> providerClass, ClassLoader classLoader) {
        return ServiceLoader.load(providerClass, classLoader).iterator();
    }

}
