package de.skuzzle.tinyplugz;

import java.util.Map;

import de.skuzzle.tinyplugz.TinyPlugzConfigurator.TinyPlugzImpl;

final class DefaultImplementationTinyPlugzLookup implements TinyPlugzLookUp {

    @Override
    @SuppressWarnings("deprecation")
    public TinyPlugz getInstance(ClassLoader classLoader, Map<Object, Object> props)
            throws TinyPlugzException {
        return new TinyPlugzImpl();
    }

}
