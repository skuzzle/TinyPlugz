package de.skuzzle.tinyplugz.util;

import java.util.Enumeration;
import java.util.Iterator;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Interface which combines {@link Iterator} and {@link Enumeration}.
 *
 * @author Simon Taddiken
 * @param <T> Type of the iterator's elements.
 * @since 0.2.0
 */
public interface ElementIterator<T> extends Iterator<T>, Enumeration<T> {

    /**
     * Wraps the given iterator into a {@link ElementIterator}.
     *
     * @param <T> Type of the iterator's elements.
     * @param iterator The iterator to wrap.
     * @return The wrapped iterator.
     */
    @NonNull
    public static <T> ElementIterator<T> wrap(Iterator<T> iterator) {
        Require.nonNull(iterator, "iterator");
        if (iterator instanceof ElementIterator<?>) {
            return (ElementIterator<T>) iterator;
        }
        return new ElementIterator<T>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }
        };
    }

    /**
     * Wraps the given enumeration into a {@link ElementIterator}.
     *
     * @param <T> Type of the iterator's elements.
     * @param enumeration The iterator to wrap.
     * @return The wrapped iterator.
     */
    @NonNull
    public static <T> ElementIterator<T> wrap(Enumeration<T> enumeration) {
        Require.nonNull(enumeration, "enumeration");
        if (enumeration instanceof ElementIterator<?>) {
            return (ElementIterator<T>) enumeration;
        }
        return new ElementIterator<T>() {

            @Override
            public boolean hasNext() {
                return enumeration.hasMoreElements();
            }

            @Override
            public T next() {
                return enumeration.nextElement();
            }
        };
    }

    @Override
    public default boolean hasMoreElements() {
        return hasNext();
    }

    @Override
    public default T nextElement() {
        return next();
    }
}
