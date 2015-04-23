package de.skuzzle.tinyplugz.guice;

import java.util.Map;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

final class DefaultInjectorFactory implements InjectorFactory {

    @Override
    public final Injector createInjector(Iterable<Module> modules,
            Map<Object, Object> props) {
        final Injector parent = (Injector) props.get(TinyPlugzGuice.PARENT_INJECTOR);
        if (parent == null) {
            return Guice.createInjector(modules);
        } else {
            return parent.createChildInjector(modules);
        }
    }

}
