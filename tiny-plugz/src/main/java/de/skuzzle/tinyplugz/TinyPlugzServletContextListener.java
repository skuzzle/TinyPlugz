package de.skuzzle.tinyplugz;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DefineProperties;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DeployTinyPlugz;

/**
 * ServletContextListener for configuring TinyPlugz for a web application. Users
 * need to extend this class to provide the actual configuration for TinyPlugz
 * by implementing {@link #configure(DefineProperties, ServletContext)}. You then need to
 * register the listener in your web.xml:
 *
 * <pre>
 * &lt;listener&gt;
 *     &lt;listener-class&gt;com.your.domain.TinyPlugzServletContextListenerImpl&lt;/listener-class&gt;
 * &lt;/listener&gt;
 * </pre>
 *
 * TinyPlugz will then be deployed when this listener receives the
 * contextInitialized event and will be undeployed when the listener receives
 * the contextDestroyed event.
 * @author Simon Taddiken
 * @see TinyPlugzFilter
 */
public abstract class TinyPlugzServletContextListener implements ServletContextListener {

    /**
     * Provides settings for deploying TinyPlugz. Sample implementation of this method:
     *
     * <pre>
     * &#064;Override
     * protected final DeployTinyPlugz configure(DefineProperties props, ServletContext context) {
     *     final String pathString = context.getRealPath("WEB-INF/plugins");
     *     final Path pluginDir = Paths.get(pathString);
     *
     *     return props.withProperty(Options.FAIL_ON_MULTIPLE_PROVIDERS)
     *             .withPlugins(source -&gt; source.addAllPluginJars(pluginDir));
     * }
     * </pre>
     *
     * @param props Builder object for specifying configuration properties.
     * @param context The current servlet context.
     * @return The {@link DeployTinyPlugz} instance which is returned by the
     *         given builder's
     *         {@link DefineProperties#withPlugins(java.util.function.Consumer)
     *         withPlugins} method.
     */
    protected abstract DeployTinyPlugz configure(DefineProperties props,
            ServletContext context);

    @Override
    public final void contextInitialized(ServletContextEvent sce) {
        final ServletContext ctx = sce.getServletContext();
        final ClassLoader webAppCl = getClass().getClassLoader();
        final DefineProperties props = TinyPlugzConfigurator.setupUsingParent(webAppCl);

        final DeployTinyPlugz config = Require.nonNullResult(
                configure(props, ctx),
                "TinyPlugzServletContextListener.configure");

        config.deploy();
    }

    @Override
    public final void contextDestroyed(ServletContextEvent sce) {
        TinyPlugz.getInstance().undeploy();
    }
}
