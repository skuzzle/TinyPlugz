package de.skuzzle.tinyplugz;

import java.util.ServiceLoader;

/**
 * Holds constants which can be passed to the {@link TinyPlugzConfigurator}
 * before deployment.
 *
 * @author Simon Taddiken
 */
public final class Options {

    /**
     * Configuration property for specifying a full qualified name of a class
     * which extends {@link TinyPlugz}. If this property is present, the default
     * lookup for an implementation using the {@link ServiceLoader} is skipped.
     * TinyPlugz will try to load the given class using the configured parent
     * Classloader (see static factory methods in {@link TinyPlugzConfigurator}
     * ).
     *
     * <p>
     * Note: The presence of this property AND {@link Options#FORCE_DEFAULT}
     * will raise an exception when
     * {@link TinyPlugzConfigurator.DeployTinyPlugz#deploy() deploying}.
     * </p>
     */
    public static final String FORCE_IMPLEMENTATION = "tinyplugz.forceImplementation";

    /**
     * Configuration property for disabling the TinyPlugz implementation lookup
     * and always use the default implementation. Every non-null value will
     * enable this feature.
     *
     * <p>
     * Note: The presence of this property AND {@link #FORCE_IMPLEMENTATION}
     * will raise an exception when
     * {@link TinyPlugzConfigurator.DeployTinyPlugz#deploy() deploying}.
     * </p>
     */
    public static final String FORCE_DEFAULT = "tinyplugz.forceDefault";

    /**
     * Configuration option which will cause the deployment to fail if there are
     * multiple service providers on the class path. If not present, only a
     * warning will be logged and an undefined provider will be chosen among the
     * available. Every non-null value will enable this feature.
     */
    public static final String FAIL_ON_MULTIPLE_PROVIDERS =
            "tinyplugz.failOnMultipleProviders";

    private Options() {
        // hidden constructor
    }
}
