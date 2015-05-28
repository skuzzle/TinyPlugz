package de.skuzzle.tinyplugz;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds constants which can be passed to the {@link TinyPlugzConfigurator}
 * before deployment.
 *
 * @author Simon Taddiken
 */
public final class Options {

    /**
     * Configuration property for explicitly specifying the {@link TinyPlugz}
     * implementation to use. It supports three different kind of value types:
     * <ul>
     * <li>If the value is an instance of TinyPlugz, then just that instance
     * will be deployed.</li>
     * <li>If the value is an instance of {@link Class}, then the class's
     * default constructor will be invoked to create a TinyPlugz instance.</li>
     * <li>If the value is a String, that String will be interpreted as a full
     * qualified name to class which implements TinyPlugz. The class will be
     * loaded by the parent Classloader and constructed via its default
     * constructor.</li>
     * </ul>
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

    /**
     * Unmodifiable set containing all known default options.
     *
     * @since 0.2.0
     */
    public static final Set<String> ALL_DEFAULT_OPTIONS;
    static {
        ALL_DEFAULT_OPTIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                FORCE_IMPLEMENTATION, FORCE_DEFAULT, FAIL_ON_MULTIPLE_PROVIDERS
                )));
    }

    private Options() {
        // hidden constructor
    }
}
