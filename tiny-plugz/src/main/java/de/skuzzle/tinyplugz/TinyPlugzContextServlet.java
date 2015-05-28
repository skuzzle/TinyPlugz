package de.skuzzle.tinyplugz;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Wraps another Servlet and performs every action ({@link #init(ServletConfig)}
 * and {@link #service(ServletRequest, ServletResponse)} in the scope of the
 * {@link TinyPlugz} Classloader as context Classloader.
 *
 * @author Simon Taddiken
 * @since 0.2.0
 */
public final class TinyPlugzContextServlet implements Servlet {

    /**
     * Wraps the given Servlet.
     *
     * @param servlet The servlet to wrap.
     * @return The decorated servlet.
     */
    public static Servlet wrap(Servlet servlet) {
        Require.nonNull(servlet, "servlet");
        if (servlet instanceof TinyPlugzContextServlet) {
            return servlet;
        }
        return new TinyPlugzContextServlet(servlet);
    }

    private final Servlet wrapped;

    private TinyPlugzContextServlet(Servlet wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try (ExchangeClassLoader exchange = ExchangeClassLoader.forTinyPlugz()) {
            this.wrapped.init(config);
        }
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException,
            IOException {
        try (ExchangeClassLoader exchange = ExchangeClassLoader.forTinyPlugz()) {
            this.wrapped.service(req, res);
        }
    }

    @Override
    public ServletConfig getServletConfig() {
        return this.wrapped.getServletConfig();
    }

    @Override
    public String getServletInfo() {
        return this.wrapped.getServletInfo();
    }

    @Override
    public void destroy() {
        this.wrapped.destroy();
    }
}
