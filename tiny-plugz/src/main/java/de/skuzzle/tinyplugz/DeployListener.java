package de.skuzzle.tinyplugz;

import java.util.Map;

/**
 * Listener which is notified when a TinyPlugz instance is deployed. Implementations of
 * this listener must be registered as a Java Service Provider.
 *
 * @author Simon Taddiken
 * @since 0.2.0
 */
public interface DeployListener {

    /**
     * Notified when TinyPlugz has been deployed.
     *
     * @param tinyPlugz The deployed instance.
     * @param properties The properties with which the instance has been configured.
     */
    void initialized(TinyPlugz tinyPlugz, Map<Object, Object> properties);
}
