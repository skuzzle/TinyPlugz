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
 * @see TinyPlugzServletContextListener
 */
public final class TinyPlugzFilter implements Filter {

    /**
     * Advises the filter to check whether the context ClassLoader has been exchanged
     * by a 3rd party during processing of the filter chain. The filter will then throw
     * an exception if it discovers that the Classloader has been exchanged and not
     * restored.
     * <p>
     * Please note that this does not discover the case in which a 3rd party component
     * exchanges the Classloader but also restores it again.
     */
    public static final String FAIL_ON_CLASSLOADER_CHANGED =
            "fail-when-classloader-changed";

    private boolean failOnChange = false;

    @Override
    public final void init(FilterConfig filterConfig) throws ServletException {
        this.failOnChange = Boolean.TRUE.toString().equalsIgnoreCase(
                filterConfig.getInitParameter(FAIL_ON_CLASSLOADER_CHANGED));
    }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        try (ExchangeClassLoader exchange = ExchangeClassLoader.forTinyPlugz()) {
            exchange.setFailOnChange(this.failOnChange);
            chain.doFilter(request, response);
        }
    }

    @Override
    public final void destroy() {
        // Nothing to do here
    }

}
