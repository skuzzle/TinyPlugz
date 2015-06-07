package de.skuzzle.tinyplugz.guice;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DefineProperties;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DeployTinyPlugz;
import de.skuzzle.tinyplugz.servlet.TinyPlugzServletContextListener;
import de.skuzzle.tinyplugz.util.Require;

/**
 * ServletContextListener which uses TinyPlugz for configuring the application's
 * guice {@link Injector}. When receiving the contextInitialized event, this
 * listener will deploy TinyPlugz using the configuration given by
 * {@link #configure(DefineProperties, ServletContext)}. It will then obtain the injector
 * from TinyPlugz and configure the {@link GuiceServletContextListener}
 * appropriately.
 * <p>
 * {@link #tinyPlugzDeployed(TinyPlugz, Injector, ServletContextEvent)} will be
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
                        ServletContext context) {

                    return TinyPlugzGuiceServletContextListener.this.configure
                            (props, context);
                }
            };

    /**
     * Configures TinyPlugz. See {@link TinyPlugzServletContextListener}.
     *
     * @param props Builder object for specifying configuration options.
     * @param context The current servlet context.
     * @return The {@link DeployTinyPlugz} instance returned by
     *         {@link DefineProperties#withPlugins(java.util.function.Consumer)}
     *         .
     */
    protected abstract DeployTinyPlugz configure(DefineProperties props,
            ServletContext context);

    /**
     * Called after TinyPlugz and guice have successfully been setup. The
     * default implementation does nothing.
     *
     * @param tinyPlugz The deployed TinyPlugz instance.
     * @param injector The created guice injector.
     * @param contextEvent The current servlet context event.
     */
    protected void tinyPlugzDeployed(TinyPlugz tinyPlugz, Injector injector,
            ServletContextEvent contextEvent) {
        // do nothing
    }

    /**
     * Called when this listener receives the context destroyed event. TinyPlugz
     * will not be deployed anymore by the time this method is called. The
     * default implementation does nothing.
     *
     * @param injector The guice injector.
     * @param contextEvent The current servlet context event.
     */
    protected void tinyPlugzUndeployed(Injector injector,
            ServletContextEvent contextEvent) {
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

        tinyPlugzDeployed(TinyPlugz.getInstance(), this.injector, servletContextEvent);
    }

    @Override
    public final void contextDestroyed(ServletContextEvent servletContextEvent) {
        // undeploy tiny plugz
        this.delegate.contextDestroyed(servletContextEvent);

        // undeploy guice
        super.contextDestroyed(servletContextEvent);

        tinyPlugzUndeployed(this.injector, servletContextEvent);
        this.injector = null;
    }
}
