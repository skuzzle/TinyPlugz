package de.skuzzle.tinyplugz.guice;

import java.util.Map;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

final class DefaultInjectorFactory implements InjectorFactory {

    @Override
    public final Injector createInjector(Iterable<Module> modules,
            Map<Object, Object> props) {
        final Injector parent = (Injector) props.get(TinyPlugzGuice.PARENT_INJECTOR);
        if (parent == null) {
            final String stageName = (String) props.get(TinyPlugzGuice.INJECTOR_STAGE);
            final Stage stage = stageName == null
                    ? Stage.PRODUCTION
                    : Stage.valueOf(stageName);
            return Guice.createInjector(stage, modules);
        } else {
            return parent.createChildInjector(modules);
        }
    }

}
