package de.skuzzle.tinyplugz.test.testplugin1;

import de.skuzzle.jeve.EventProvider;
import de.skuzzle.jeve.stores.DefaultListenerStore;
import de.skuzzle.tinyplugz.Require;
import de.skuzzle.tinyplugz.TinyPlugz;

public class ClassWithPlugin1Dependency {

    public static EventProvider<DefaultListenerStore> provider =
            EventProvider.createDefault();

    static {
        final ClassLoader cl = TinyPlugz.getInstance().getClassLoader();
        try {
            final Class<?> cls = cl.loadClass("de.skuzzle.jeve.EventProvider");
            Require.state(false, "Class should not have been found");
        } catch (ClassNotFoundException e) {
        }
    }
}
