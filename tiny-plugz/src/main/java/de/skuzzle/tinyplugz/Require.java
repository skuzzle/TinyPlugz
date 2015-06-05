package de.skuzzle.tinyplugz;

import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;

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
     * @param <T> Type of the obejct to test.
     * @param obj The object to test.
     * @param paramName The name of the parameter (used for exception message).
     * @return The object.
     * @throws IllegalArgumentException If {@code obj} is null.
     */
    @NonNull
    public static <T> T nonNull(@NonNull T obj, String paramName) {
        if (obj == null) {
            throw new IllegalArgumentException(String.format(
                    "'%s' must not be null", paramName));
        }
        return obj;
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

    /**
     * Asserts that the given condition holds <code>true</code> and throws a
     * custom exception if not.
     *
     * @param state The condition to check.
     * @param excpetionCtor Constructor of the exception to throw.
     * @param message String for exception message.
     * @param format Formatting parameters for the exception message.
     * @throws T If {@code condition} is <code>false</code>.
     * @since 0.2.0
     */
    public static <T extends Exception> void state(boolean state,
            Function<String, T> excpetionCtor, String message, Object format) throws T {
        if (!state) {
            throw excpetionCtor.apply(String.format(message, format));
        }
    }

    /**
     * Asserts that a method call yielded a non-null result.
     *
     * @param <T> Type of the object to test.
     * @param result The result object.
     * @param call A String description of the call, like "Object.calledMethod".
     * @return The object which was passed in.
     * @throws IllegalStateException If {@code result} is <code>null</code>.
     */
    @NonNull
    public static <T> T nonNullResult(@NonNull T result, String call) {
        if (result == null) {
            // XXX: IllegalStateException might not be the best choice
            throw new IllegalStateException(String.format(
                    "call of '%s' yielded unexpected null value", call));
        }
        return result;
    }
}
