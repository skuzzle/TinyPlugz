package de.skuzzle.tinyplugz.guice;

import java.nio.file.Path;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

import de.skuzzle.tinyplugz.Require;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DefineProperties;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DeployTinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzServletContextListener;

/**
 * ServletContextListener which uses TinyPlugz for configuring the application's
 * guice {@link Injector}. When receiving the contextInitialized event, this
 * listener will deploy TinyPlugz using the configuration given by
 * {@link #configure(DefineProperties, Path)}. It will then obtain the injector
 * from TinyPlugz and configure the {@link GuiceServletContextListener}
 * appropriately.
 * <p>
 * {@link #tinyPlugzDeployed(TinyPlugz, Injector, ServletContext)} will be
 * called if TinyPlugz and guice have successfully been setup. Its default
 * implementation does nothing and may be overridden e.g. to put additional
 * information into the servlet context.
 *
 * @author Simon Taddiken
 * @see TinyPlugzServletContextListener
 */
public abstract class TinyPlugzGuiceServletContextListener extends
        GuiceServletContextListener {

    private Injector injector;
    private final TinyPlugzServletContextListener delegate =
            new TinyPlugzServletContextListener() {

                @Override
                protected final DeployTinyPlugz configure(DefineProperties props,
                        Path webInfDir) {

                    return TinyPlugzGuiceServletContextListener.this.configure
                            (props, webInfDir);
                }
            };

    /**
     * Configures TinyPlugz. See {@link TinyPlugzServletContextListener}.
     *
     * @param props Builder object for specifying configuration options.
     * @param webInfDir The web application's WEB-INF directory.
     * @return The {@link DeployTinyPlugz} instance returned by
     *         {@link DefineProperties#withPlugins(java.util.function.Consumer)}
     *         .
     */
    protected abstract DeployTinyPlugz configure(DefineProperties props, Path webInfDir);

    /**
     * Called after TinyPlugz and guice have successfully been setup. The
     * default implementation does nothing.
     *
     * @param tinyPlugz The deployed TinyPlugz instance.
     * @param injector The created guice injector.
     * @param ctx The current servlet context.
     */
    protected void tinyPlugzDeployed(TinyPlugz tinyPlugz, Injector injector,
            ServletContext ctx) {
        // do nothing
    }

    /**
     * Called when this listener receives the context destroyed event. TinyPlugz will not
     * be deployed anymore by the time this method is called. The default implementation
     * does nothing.
     *
     * @param ctx The destroyed servlet context.
     */
    protected void tinyPlugzUndeployed(ServletContext ctx) {
        // do nothing
    }

    /**
     * Returns the guice {@link Injector} which will be created by TinyPlugz.
     * The injector will be made available when this listener receives the
     * context initialized event.
     *
     * @return The guice Injector to use within the web application.
     */
    @Override
    protected final Injector getInjector() {
        Require.state(this.injector != null, "Injector not initialized");
        return this.injector;
    }

    @Override
    public final void contextInitialized(ServletContextEvent servletContextEvent) {
        // this deploys TinyPlugz
        this.delegate.contextInitialized(servletContextEvent);

        this.injector = TinyPlugz.getInstance().getService(Injector.class);
        super.contextInitialized(servletContextEvent);

        tinyPlugzDeployed(TinyPlugz.getInstance(), this.injector,
                servletContextEvent.getServletContext());
    }

    @Override
    public final void contextDestroyed(ServletContextEvent servletContextEvent) {
        // undeploy tiny plugz
        this.delegate.contextDestroyed(servletContextEvent);

        // undeploy guice
        super.contextDestroyed(servletContextEvent);

        tinyPlugzUndeployed(servletContextEvent.getServletContext());
        this.injector = null;
    }
}
