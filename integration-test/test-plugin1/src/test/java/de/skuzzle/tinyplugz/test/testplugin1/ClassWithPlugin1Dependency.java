package de.skuzzle.tinyplugz.test.testplugin1;

import de.skuzzle.jeve.EventProvider;
import de.skuzzle.jeve.stores.DefaultListenerStore;

public class ClassWithPlugin1Dependency {

    public static EventProvider<DefaultListenerStore> provider =
            EventProvider.createDefault();
}
