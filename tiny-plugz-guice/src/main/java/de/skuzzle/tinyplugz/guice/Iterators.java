package de.skuzzle.tinyplugz.guice;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import de.skuzzle.tinyplugz.Require;

final class Iterators {

    private Iterators() {}

    @SafeVarargs
    public static <T> Iterator<T> wrap(Iterator<T>... iterators) {
        Require.nonNull(iterators, "iterators");
        if (iterators.length == 0) {
            return Collections.emptyIterator();
        } else if (iterators.length == 1) {
            return iterators[0];
        }
        return new CompoundIterator<>(iterators);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> Iterable<T> wrap(Iterable<T>...iterables) {
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

        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return it;
            }
        };
    }

    /**
     * An {@link Iterator} implementation which internally iterates an array of
     * enumerations.
     *
     * @author Simon Taddiken
     * @param <E> Type of elements returned by this Iterator.
     */
    private static final class CompoundIterator<E> implements Iterator<E> {
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
