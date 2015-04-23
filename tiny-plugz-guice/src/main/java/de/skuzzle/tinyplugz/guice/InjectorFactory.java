package de.skuzzle.tinyplugz.guice;

import java.util.Map;

import com.google.inject.Injector;
import com.google.inject.Module;

import de.skuzzle.tinyplugz.TinyPlugzConfigurator;

/**
 * Gives the user full control over how the Injector used by
 * {@link TinyPlugzGuice} will be created. An instance of this class can be
 * passed as value for the property {@link TinyPlugzGuice#INJECTOR_FACTORY} to
 * the {@link TinyPlugzConfigurator}
 *
 * @author Simon Taddiken
 */
public interface InjectorFactory {

    /**
     * Creates the injector to be used by {@link TinyPlugzGuice}. This method
     * will be called once when TinyPlugz is deployed. The passed modules are
     * the ones pulled from plugins as well as additional modules (see
     * TinyPlugzGuice documentation).
     *
     * @param modules The modules for setting up the injector.
     * @param props TinyPlugz configuration properties.
     * @return The injector.
     */
    public Injector createInjector(Iterable<Module> modules, Map<Object, Object> props);
}
