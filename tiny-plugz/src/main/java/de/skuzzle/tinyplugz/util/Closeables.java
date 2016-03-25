package de.skuzzle.tinyplugz.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for safely closing {@link Closeable} instances without throwing
 * an exception.
 *
 * @author Simon Taddiken
 * @since 0.3.0
 */
public final class Closeables {

    private static final Logger LOG = LoggerFactory.getLogger(Closeables.class);

    private Closeables() {
        // hidden
    }

    /**
     * Closes the given Closeables and collects all occurring exceptions as
     * suppressed exception to the exception that occurred first.
     *
     * @param closeables The Closeables to close.
     * @throws IOException If any of them throws an IOException.
     */
    public static void close(Closeable... closeables) throws IOException {
        close(Arrays.asList(closeables));
    }

    /**
     * Closes the given Closeables and collects all occurring exceptions as
     * suppressed exception to the exception that occurred first.
     *
     * @param closeables The Closeables to close.
     * @throws IOException If any of them throws an IOException.
     */
    public static void close(Iterable<? extends Closeable> closeables)
            throws IOException {
        IOException first = null;
        for (final Closeable c : closeables) {
            if (c == null) {
                LOG.debug("trying to call .close() on null reference");
                continue;
            }
            try {
                c.close();
            } catch (final IOException e) {
                if (first == null) {
                    first = e;
                } else {
                    first.addSuppressed(e);
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }

    /**
     * Closes the given {@link Closeable} and logs any error that occurs.
     *
     * @param c The {@link Closeable}. Might be <code>null</code>.
     * @return Whether closing was successful. That is, the input was not
     *         <code>null</code> and its {@link Closeable#close()} method did
     *         not throw an exception.
     */
    public static boolean safeClose(Closeable c) {
        if (c == null) {
            LOG.debug("trying to call .close() on null reference");
            return true;
        }
        try {
            c.close();
            return true;
        } catch (final IOException e) {
            LOG.error("Error while closing '{}': ", c, e);
            return false;
        }
    }

    /**
     * Closes all of the given {@link Closeable Closeables} by calling
     * {@link #safeClose(Closeable)} for each element in the array.
     *
     * @param closeables The array of Closeables.
     * @return Whether closing of all Closables was successful.
     */
    public static boolean safeCloseAll(Closeable... closeables) {
        boolean result = true;
        for (final Closeable c : closeables) {
            result &= safeClose(c);
        }
        return result;
    }

    /**
     * Closes all of the given {@link Closeable Closeables} by calling
     * {@link #safeClose(Closeable)} for each element returned by the Iterable.
     *
     * @param closeables The of Closeables.
     * @return Whether closing of all Closables was successful.
     */
    public static boolean safeCloseAll(Iterable<? extends Closeable> closeables) {
        final Iterator<? extends Closeable> it = closeables.iterator();
        boolean result = true;
        while (it.hasNext()) {
            result &= safeClose(it.next());
        }
        return result;
    }
}
