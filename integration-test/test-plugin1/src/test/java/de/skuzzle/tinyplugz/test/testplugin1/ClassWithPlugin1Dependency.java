package de.skuzzle.tinyplugz.test.testplugin1;

import de.skuzzle.jeve.EventProvider;
import de.skuzzle.jeve.stores.DefaultListenerStore;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.util.Require;

public class ClassWithPlugin1Dependency {

    public static EventProvider<DefaultListenerStore> provider =
            EventProvider.createDefault();

    static {
        final ClassLoader cl = TinyPlugz.getInstance().getClassLoader();
        try {
            cl.loadClass("de.skuzzle.jeve.EventProvider");
            Require.state(false, "Class should not have been found");
        } catch (final ClassNotFoundException e) {
        }
        final ClassLoader self = ClassWithPlugin1Dependency.class.getClassLoader();
        try {
            final Class<?> cls = self.loadClass("de.skuzzle.jeve.EventProvider");
            Require.condition(cls.getClassLoader() != self,
                    "Class should have been loaded by the dependency classloader");
            Require.condition(cls.getClassLoader().getParent() == self.getParent(),
                    "Parent of dependency classloader should be the app classloader");
        } catch (final ClassNotFoundException e) {
            Require.state(false, "Class should have been found");
        }
    }
}
