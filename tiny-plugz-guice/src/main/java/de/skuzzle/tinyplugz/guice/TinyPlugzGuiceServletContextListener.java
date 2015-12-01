package de.skuzzle.tinyplugz.guice;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

import de.skuzzle.tinyplugz.DeployHook;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DefineDeployHook;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DefineProperties;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DeployTinyPlugz;
import de.skuzzle.tinyplugz.servlet.TinyPlugzServletContextListener;
import de.skuzzle.tinyplugz.util.Require;

/**
 * ServletContextListener which uses TinyPlugz for configuring the application's
 * guice {@link Injector}. When receiving the contextInitialized event, this
 * listener will deploy TinyPlugz using the configuration given by
 * {@link #configure(DefineProperties, ServletContext)}. It will then obtain the
 * injector from TinyPlugz and configure the {@link GuiceServletContextListener}
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

    static final String SERVLET_CONTEXT_KEY = ServletContext.class.getName();
    private Injector injector;

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
        final ServletContext ctx = servletContextEvent.getServletContext();
        final ClassLoader webAppCl = getClass().getClassLoader();
        final DefineProperties props = TinyPlugzConfigurator.setupUsingParent(webAppCl);

        // Cast is safe as by TinyPlugz specification. The hook serves for
        // proper initialization of guice and guice servlet extension before
        // notifying the DeployListeners
        final DefineDeployHook defineHook = (DefineDeployHook) props;
        defineHook.setDeployHook(new GuiceDeployHook(servletContextEvent));

        final DeployTinyPlugz config = Require.nonNullResult(
                configure(props, ctx),
                "TinyPlugzServletContextListener.configure");
        config.deploy();

        tinyPlugzDeployed(TinyPlugz.getInstance(), this.injector, servletContextEvent);
    }

    @Override
    public final void contextDestroyed(ServletContextEvent servletContextEvent) {
        // undeploy tiny plugz
        if (TinyPlugz.isDeployed()) {
            TinyPlugz.getInstance().undeploy();
        }

        // undeploy guice
        super.contextDestroyed(servletContextEvent);

        tinyPlugzUndeployed(this.injector, servletContextEvent);
        this.injector = null;
    }

    private class GuiceDeployHook implements DeployHook {
        private final ServletContextEvent servletContextEvent;

        private GuiceDeployHook(ServletContextEvent servletContextEvent) {
            this.servletContextEvent = servletContextEvent;
        }

        @Override
        public void beforeDeployment(TinyPlugz instance, Map<Object, Object> properties) {
            TinyPlugzGuiceServletContextListener.this.injector =
                    instance.getService(Injector.class);
        }

        @Override
        public void beforeNotifyListener(TinyPlugz instance,
                Map<Object, Object> properties) {

            TinyPlugzGuiceServletContextListener.super.contextInitialized(
                    this.servletContextEvent);
        }

        @Override
        public void beforeCreateInstance(Map<Object, Object> properties) {

        }

    }
}
