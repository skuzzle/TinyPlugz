package de.skuzzle.tinyplugz;

public class TinyPlugzException extends Exception {

    /** */
    private static final long serialVersionUID = -210615746197934170L;

    public TinyPlugzException() {
        super();
    }

    public TinyPlugzException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public TinyPlugzException(String message, Throwable cause) {
        super(message, cause);
    }

    public TinyPlugzException(String message) {
        super(message);
    }

    public TinyPlugzException(Throwable cause) {
        super(cause);
    }

}
