package de.skuzzle.tinyplugz;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Sets the TinyPlugz classloader as context classloader to the current thread
 * before processing the filter chain and restores the previous context
 * classloader afterwards. This filter can be employed even if TinyPlugz is not already
 * deployed. In that case, the filter delegates to the filter chain without further
 * actions.
 * <p>
 * Exchanging the context class loader allows frameworks like JSF to look up resources
 * that reside in plugins loaded by TinyPlugz.
 *
 * @author Simon Taddiken
 * @since 0.2.0
 */
public final class TinyPlugzFilter implements Filter {

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (TinyPlugz.isDeployed()) {
            final ClassLoader tinyPlugzCl = TinyPlugz.getInstance().getClassLoader();
            final ClassLoader backupCl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(tinyPlugzCl);
                chain.doFilter(request, response);
            } finally {
                Thread.currentThread().setContextClassLoader(backupCl);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public final void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public final void destroy() {}

}
