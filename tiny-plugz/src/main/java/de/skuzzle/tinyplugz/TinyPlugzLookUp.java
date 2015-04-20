package de.skuzzle.tinyplugz;

import java.util.Map;

interface TinyPlugzLookUp {

    public TinyPlugz getInstance(ClassLoader classLoader, Map<Object, Object> props)
            throws TinyPlugzException;
}
