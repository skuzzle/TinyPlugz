package de.skuzzle.tinyplugz;

import java.net.URL;
import java.net.URLClassLoader;

class TinyPlugzClassLoader extends URLClassLoader {

    public TinyPlugzClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
    }

    @Override
    public URL getResource(String name) {
        return super.getResource(name);
    }
}
