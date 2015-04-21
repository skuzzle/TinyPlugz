package de.skuzzle.tinyplugz;

/**
 * Base for TinyPlugz exception. Thrown when TinyPlugz configuration or
 * deployment failed.
 *
 * @author Simon Taddiken
 */
public class TinyPlugzException extends Exception {

    /** */
    private static final long serialVersionUID = -210615746197934170L;

    /**
     * Constructs a new exception with null as its detail message. The cause is
     * not initialized, and may subsequently be initialized by a call to
     * initCause.
     */
    public TinyPlugzException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message, cause,
     * suppression enabled or disabled, and writable stack trace enabled or
     * disabled.
     *
     * @param message the detail message.
     * @param cause the cause (A null value is permitted, and indicates that the
     *            cause is nonexistent or unknown.).
     * @param enableSuppression whether or not suppression is enabled or
     *            disabled.
     * @param writableStackTrace whether or not the stack trace should be
     *            writable.
     */
    public TinyPlugzException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * Note that the detail message associated with cause is not automatically
     * incorporated in this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by
     *            the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A null value is permitted, and
     *            indicates that the cause is nonexistent or unknown.)
     */
    public TinyPlugzException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified detail message. The cause
     * is not initialized, and may subsequently be initialized by a call to
     * {@link #initCause(Throwable)}.
     *
     * @param message the detail message. The detail message is saved for later
     *            retrieval by the {@link #getMessage()} method.
     */
    public TinyPlugzException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause and a detail message
     * of (cause==null ? null : cause.toString()) (which typically contains the
     * class and detail message of cause). This constructor is useful for
     * exceptions that are little more than wrappers for other throwables (for
     * example, {@link java.security.PrivilegedActionException}).
     *
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A null value is permitted, and
     *            indicates that the cause is nonexistent or unknown.)
     */
    public TinyPlugzException(Throwable cause) {
        super(cause);
    }
}
