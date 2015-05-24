package de.skuzzle.tinyplugz;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DefineProperties;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DeployTinyPlugz;

/**
 * ServletContextListener for configuring TinyPlugz for a web application. Users
 * need to extend this class to provide the actual configuration for TinyPlugz
 * by implementing {@link #configure(DefineProperties, Path)}. You then need to
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
 *
 * @author Simon Taddiken
 */
public abstract class TinyPlugzServletContextListener implements ServletContextListener {

    private static final String WEB_INF = "WEB-INF";

    /**
     * Provides settings for deploying TinyPlugz. The passed in path is the
     * location of the web application's WEB-INF directory and may be used for
     * looking up plugins. Sample implementation of this method:
     *
     * <pre>
     * &#064;Override
     * protected final DeployTinyPlugz configure(DefineProperties props, Path webInfDir) {
     *     final Path pluginDir = webInfDir.resolve(&quot;plugins&quot;);
     *
     *     return props.withProperty(Options.FAIL_ON_MULTIPLE_PROVIDERS)
     *             .withPlugins(source -&gt; source.addAllPluginJars(pluginDir));
     * }
     * </pre>
     *
     * @param props Builder object for specifying configuration properties.
     * @param webInfDir The location of the WEB-INF directory.
     * @return The {@link DeployTinyPlugz} instance which is returned by the
     *         given builder's
     *         {@link DefineProperties#withPlugins(java.util.function.Consumer)
     *         withPlugins} method.
     */
    protected abstract DeployTinyPlugz configure(DefineProperties props, Path webInfDir);

    @Override
    public final void contextInitialized(ServletContextEvent sce) {
        final ServletContext ctx = sce.getServletContext();
        final Path webInfDir = Paths.get(ctx.getRealPath(WEB_INF));

        final ClassLoader webAppCl = getClass().getClassLoader();
        final DefineProperties props = TinyPlugzConfigurator.setupUsingParent(webAppCl);
        final DeployTinyPlugz config = configure(props, webInfDir);

        config.deploy();
    }

    @Override
    public final void contextDestroyed(ServletContextEvent sce) {
        TinyPlugz.getInstance().undeploy();
    }

}