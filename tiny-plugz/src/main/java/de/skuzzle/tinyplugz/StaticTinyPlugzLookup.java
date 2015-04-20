package de.skuzzle.tinyplugz;

import java.util.Map;

final class StaticTinyPlugzLookup implements TinyPlugzLookUp {

    @Override
    public TinyPlugz getInstance(ClassLoader classLoader, Map<Object, Object> props)
            throws TinyPlugzException {
        final String className = props.get(TinyPlugzConfigurator.FORCE_IMPLEMENTATION)
                .toString();

        // as by precondition check in the configurator.
        assert className != null;

        try {
            final Class<?> cls = classLoader.loadClass(className);
            if (!TinyPlugz.class.isAssignableFrom(cls)) {
                throw new TinyPlugzException(String.format(
                        "'%s' does not extend TinyPlugz", cls.getName()));
            }
            return (TinyPlugz) cls.newInstance();
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException e) {
            throw new TinyPlugzException(
                    "Error while instantiating static TinyPlugz implementation", e);
        }
    }

}
