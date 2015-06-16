package de.skuzzle.tinyplugz.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import de.skuzzle.tinyplugz.TinyPlugzException;

/**
 * Java Reflection utility methods.
 *
 * @author Simon Taddiken
 */
public final class ReflectionUtil {

    private ReflectionUtil() {
        // hidden constructor.
    }

    /**
     * Creates or returns an instance of an arbitrary type which is a sub type
     * of given {@code base} class. If the source object already is an instance
     * of {@code base}, then it is returned. If the source object is a
     * {@link Class} object, its no-argument default constructor will be called
     * and the resulting object returned. If the source is a String it is taken
     * to be the full qualified name of a class which is then loaded using the
     * given ClassLoader prior to calling its default constructor.
     *
     * @param source Either a String, a Class or a ready to use object.
     * @param base A super type of the object to create.
     * @param classLoader The ClassLoader that should be used to load the class
     *            in case the {@code source} parameter is a String.
     * @return The obtained object.
     * @throws TinyPlugzException If anything goes wrong in the process
     *             described above.
     */
    public static <T> T createInstance(Object source, Class<T> base,
            ClassLoader classLoader) {
        Require.nonNull(source, "source");
        Require.nonNull(base, "base");
        Require.nonNull(classLoader, "classLoader");

        if (source instanceof Class<?>) {
            return fromClass(base, (Class<?>) source);
        } else if (source instanceof String) {
            try {
                final Class<?> concrete = classLoader.loadClass(source.toString());
                return fromClass(base, concrete);
            } catch (ClassNotFoundException e) {
                throw new TinyPlugzException(e);
            }
        } else if (base.isInstance(source)) {
            return base.cast(source);
        } else {
            throw new TinyPlugzException(String.format("'%s' is not valid for '%s'",
                    source, base.getName()));
        }
    }

    private static <T> T fromClass(Class<T> base, Class<?> concrete) {
        if (!base.isAssignableFrom(concrete)) {
            throw new TinyPlugzException(String.format("'%s' is not an instance of '%s'",
                    concrete.getName(), base.getName()));
        }
        try {
            final Constructor<?> ctor = concrete.getConstructor();
            ctor.setAccessible(true);
            return base.cast(ctor.newInstance());
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                | SecurityException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new TinyPlugzException(e);
        }
    }

}
