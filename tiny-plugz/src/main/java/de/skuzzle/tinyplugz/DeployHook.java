package de.skuzzle.tinyplugz;

import java.util.Map;

import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DefineDeployHook;

/**
 * This hook is meant to be implemented by extensions to have the possibility to
 * modify the deployment process on a finer grained base than would be possible
 * with a {@link DeployListener}.
 * <p>
 * To employ an instance, cast the result of the fluent configurator API to
 * {@link DefineDeployHook} like in the following example:
 *
 * <pre>
 * DefineProperties props = TinyPlugzConfigurator.setup()
 *         .withProperty(...);
 * ((DefineDeployHook) props).setDeployHook(yourDeployHook);
 *
 * // go on with configuration
 * props.deploy();
 * </pre>
 *
 * @author Simon Taddiken
 * @since 0.4.0
 */
public interface DeployHook {

    /**
     * Called before instantiating the TinyPlugz instance from the given
     * properties. Properties might be modified.
     *
     * @param properties The user supplied properties.
     */
    void beforeCreateInstance(Map<Object, Object> properties);

    /**
     * Called after the TinyPlugz instance has been created but before it is
     * being deployed. Properties might not be modified within this method.
     *
     * @param instance The instance that will be deployed.
     * @param properties The user supplied properties.
     */
    void beforeDeployment(TinyPlugz instance, Map<Object, Object> properties);

    /**
     * Called right after the global TinyPlugz instance has been deployed but
     * before {@link DeployListener} are notified. Properties might not be
     * modified within this method.
     *
     * @param instance The instance that has been deployed.
     * @param properties The user supplied properties.
     */
    void beforeNotifyListener(TinyPlugz instance, Map<Object, Object> properties);
}
