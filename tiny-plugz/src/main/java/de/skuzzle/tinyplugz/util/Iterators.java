package de.skuzzle.tinyplugz.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Utility class for dealing with Iterators.
 *
 * @author Simon Taddiken
 */
@NonNullByDefault({DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE})
public final class Iterators {

    private Iterators() {
        // hidden constructor
    }

    /**
     * Creates an Iterator over the single given element.
     *
     * @param t The element.
     * @return Iterator returning the single element.
     */
    public static <T> Iterator<T> singleIterator(T t) {
        Require.nonNull(t, "t");
        return Collections.singleton(t).iterator();
    }

    /**
     * Wraps the given Iterator into an Iterable.
     *
     * @param it The Iterator to wrap.
     * @return An {@link Iterable} which returns the given Iterator.
     */
    public static <T> Iterable<T> iterableOf(Iterator<T> it) {
        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return it;
            }
        };
    }

    /**
     * Creates an iterator which subsequently iterates over all given iterators.
     *
     * @param iterators An array of iterators.
     * @return A composite iterator iterating the values of all given iterators
     *         in given order.
     */
    @SafeVarargs
    public static <T> ElementIterator<T> composite(Iterator<T>... iterators) {
        Require.nonNull(iterators, "iterators");
        final Iterator<T> result;
        if (iterators.length == 0) {
            result = Collections.emptyIterator();
        } else if (iterators.length == 1) {
            if (iterators[0] instanceof ElementIterator<?>) {
                return (ElementIterator<T>) iterators[0];
            }
            result = iterators[0];
        } else {
            result = new CompoundIterator<>(iterators);
        }
        return ElementIterator.wrap(result);

    }

    /**
     * Creates an iterable which returns a composite iterator over all iterators
     * of the given iterables.
     *
     * @param iterables The Iterables to wrap.
     * @return A composite iterable.
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> Iterable<T> composite(Iterable<T>...iterables) {
        Require.nonNull(iterables, "iterables");
        final Iterator<T> it;
        if (iterables.length == 0) {
            it = Collections.emptyIterator();
        } else if (iterables.length == 1) {
            return iterables[0];
        } else {
            final Iterator<T>[] iterators = Arrays.stream(iterables)
                    .map(Iterable::iterator)
                    .toArray(size -> new Iterator[size]);
            it = new CompoundIterator<T>(iterators);
        }
        return iterableOf(it);
    }

    /**
     * An {@link ElementIterator} implementation which internally iterates an array of
     * enumerations.
     *
     * @author Simon Taddiken
     * @param <E> Type of elements returned by this Iterator.
     */
    private static final class CompoundIterator<E> implements ElementIterator<E> {
        /** Array of Iterators to iterate */
        private final Iterator<E>[] iterators;

        /** Index within the iterators array */
        private int i;

        /**
         * Creates a new Iterator which consecutively provides the elements from
         * all Iterators in the provided array.
         *
         * @param iterators The array of iterators to iterate through.
         */
        @SafeVarargs
        private CompoundIterator(Iterator<E>... iterators) {
            this.iterators = iterators;
        }

        @Override
        public boolean hasNext() {
            for (; this.i < this.iterators.length; ++this.i) {
                final Iterator<E> nextEnum = this.iterators[this.i];
                if (nextEnum.hasNext()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public E next() {
            if (hasNext()) {
                final Iterator<E> nextIt = this.iterators[this.i];
                return nextIt.next();
            }
            throw new NoSuchElementException();
        }
    }
}
