package de.skuzzle.tinyplugz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to define a scoped action during which the context Classloader is
 * exchanged for another one. After the action has been done, the previous
 * Classloader will be restored. Use this in a try-resources block like:
 *
 * <pre>
 * try (ExchangeClassLoader exchange = ExchangeClassLoader.forTinyPlugz()) {
 *     // perform scoped actions here
 * }
 * </pre>
 *
 * @author Simon Taddiken
 * @since 0.2.0
 */
public final class ExchangeClassLoader implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ExchangeClassLoader.class);

    private final ClassLoader backupCl;
    private final ClassLoader exchangeCl;

    private boolean failOnChange;

    private ExchangeClassLoader(ClassLoader classLoader) {
        this.exchangeCl = classLoader;
        this.backupCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.exchangeCl);
    }

    /**
     * Exchanges the context ClassLoader with the given one until
     * {@link #close()} is called.
     *
     * @param classLoader The Classloader to employ.
     * @return The {@link ExchangeClassLoader} object to use in a try-resources
     *         block.
     */
    public static ExchangeClassLoader with(ClassLoader classLoader) {
        Require.nonNull(classLoader, "classLoader");
        return new ExchangeClassLoader(classLoader);
    }

    /**
     * If {@link TinyPlugz} is deployed, the context ClassLoader is exchanged
     * for TinyPlugz's Classloader. Otherwise, it will not be changed.
     *
     * @return The {@link ExchangeClassLoader} object to use in a try-resources
     *         block.
     */
    public static ExchangeClassLoader forTinyPlugz() {
        if (TinyPlugz.isDeployed()) {
            return new ExchangeClassLoader(TinyPlugz.getInstance().getClassLoader());
        }
        return new ExchangeClassLoader(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Sets whether {@link #close()} throws an exception if it detects that the
     * context Classloader has been exchanged by a 3rd party.
     *
     * @param failOnChange Whether to fail on unexpected Classloader exchange.
     */
    public void setFailOnChange(boolean failOnChange) {
        this.failOnChange = failOnChange;
    }

    @Override
    public final void close() {
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.backupCl);
        if (current != this.backupCl) {
            LOG.warn("Detected 3rd party ClassLoader exchange");
            Require.state(!this.failOnChange, "Detected 3rd party ClassLoader exchange");
        }
    }

}
