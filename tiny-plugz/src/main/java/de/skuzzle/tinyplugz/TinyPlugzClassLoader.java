package de.skuzzle.tinyplugz;

import java.net.URL;
import java.net.URLClassLoader;

class TinyPlugzClassLoader extends URLClassLoader {

    TinyPlugzClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

}
