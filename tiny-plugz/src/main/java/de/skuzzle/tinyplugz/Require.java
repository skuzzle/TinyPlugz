package de.skuzzle.tinyplugz;

/**
 * Utility class for checking parameters and states.
 *
 * @author Simon Taddiken
 */
public final class Require {

    private Require() {
        // hidden constructor
    }

    /**
     * Asserts that the given object is non null.
     *
     * @param obj The object to test.
     * @param paramName The name of the parameter (used for exception message).
     * @throws IllegalArgumentException If {@code obj} is null.
     */
    public static void nonNull(Object obj, String paramName) {
        if (obj == null) {
            throw new IllegalArgumentException(String.format(
                    "'%s' must not be null", paramName));
        }
    }

    /**
     * Asserts that the given condition holds <code>true</code>.
     *
     * @param condition The condition to check.
     * @param message String for the exception message.
     * @param format Formatting parameters for the exception message.
     * @throws IllegalArgumentException If {@code condition} is
     *             <code>false</code>.
     */
    public static void condition(boolean condition, String message, Object... format) {
        if (!condition) {
            throw new IllegalArgumentException(String.format(message, format));
        }
    }

    /**
     * Asserts that the given condition holds <code>true</code>.
     *
     * @param state The condition to check.
     * @param message String for the exception message.
     * @param format Formatting parameters for the exception message.
     * @throws IllegalStateException If {@code condition} is <code>false</code>.
     */
    public static void state(boolean state, String message, Object... format) {
        if (!state) {
            throw new IllegalStateException(String.format(message, format));
        }
    }
}
