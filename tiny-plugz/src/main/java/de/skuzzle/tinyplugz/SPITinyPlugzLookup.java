package de.skuzzle.tinyplugz;

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.skuzzle.tinyplugz.TinyPlugzConfigurator.TinyPlugzImpl;

final class SPITinyPlugzLookup implements TinyPlugzLookUp {

    private static final Logger LOG = LoggerFactory.getLogger(TinyPlugzLookUp.class);

    @Override
    @SuppressWarnings("deprecation")
    public TinyPlugz getInstance(ClassLoader classLoader, Map<Object, Object> props) {
        final Iterator<TinyPlugz> providers = ServiceLoader
                .load(TinyPlugz.class, classLoader)
                .iterator();

        final TinyPlugz impl = providers.hasNext()
                ? providers.next()
                : new TinyPlugzImpl();

        if (providers.hasNext()) {
            LOG.warn("Multiple TinyPlugz bindings found on class path");
            providers.forEachRemaining(provider ->
                    LOG.debug("Ignoring TinyPlugz provider '{}'",
                            provider.getClass().getName()));
        }
        return impl;
    }

}
