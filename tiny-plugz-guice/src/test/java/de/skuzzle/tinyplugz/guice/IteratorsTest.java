package de.skuzzle.tinyplugz.guice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

public class IteratorsTest {

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testWrapNullIterator() {
        Iterators.wrap((Iterator[]) null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testWrapNullIterable() {
        Iterators.wrap((Iterable[]) null);
    }

    @Test
    public void testWrapSingleIterator() throws Exception {
        final Iterator<String> it = Collections.emptyIterator();
        assertSame(it, Iterators.wrap(it));
    }

    @Test
    public void testWrapIterables() throws Exception {
        final Collection<String> first = Arrays.asList("a", "b");
        final Collection<String> second = Arrays.asList("c", "d");
        final Iterable<String> wrapped = Iterators.wrap(first, second);
        final Iterator<String> it = wrapped.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertEquals("d", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testWrapSingleIterable() throws Exception {
        final Iterable<String> it = Collections.emptyList();
        assertSame(it, Iterators.wrap(it));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAllEmpty() {
        final Collection<String> v = new ArrayList<>();
        final Iterator<String>[] enums = (Iterator<String>[])
                new Iterator<?>[] { v.iterator(), v.iterator(), v.iterator() };
        final Iterator<String> ce = Iterators.wrap(enums);
        Assert.assertFalse(ce.hasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmptyArray() {
        final Iterator<String>[] enums = (Iterator<String>[]) new Iterator<?>[0];
        final Iterator<String> ce = Iterators.wrap(enums);
        Assert.assertFalse(ce.hasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMiddleIsEmpty() {
        final Collection<String> empty = new ArrayList<>();
        final Collection<String> oneElement = Arrays.asList("foo");
        final Iterator<String>[] enums = (Iterator<String>[])
                new Iterator<?>[] { oneElement.iterator(), empty.iterator(),
                        oneElement.iterator() };
        final Iterator<String> ce = Iterators.wrap(enums);
        Assert.assertEquals("foo", ce.next());
        Assert.assertTrue(ce.hasNext());
        Assert.assertEquals("foo", ce.next());
        Assert.assertFalse(ce.hasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFirstIsEmpty() {
        final Collection<String> empty = new ArrayList<>();
        final Collection<String> oneElement = Arrays.asList("foo");
        final Iterator<String>[] enums = (Iterator<String>[])
                new Iterator<?>[] { empty.iterator(), oneElement.iterator(),
                        oneElement.iterator() };
        final Iterator<String> ce = Iterators.wrap(enums);
        Assert.assertEquals("foo", ce.next());
        Assert.assertTrue(ce.hasNext());
        Assert.assertEquals("foo", ce.next());
        Assert.assertFalse(ce.hasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLastIsEmpty() {
        final Collection<String> empty = new ArrayList<>();
        final Collection<String> oneElement = Arrays.asList("foo");
        final Iterator<String>[] enums = (Iterator<String>[])
                new Iterator<?>[] { oneElement.iterator(), oneElement.iterator(),
                        empty.iterator() };
        final Iterator<String> ce = Iterators.wrap(enums);
        Assert.assertEquals("foo", ce.next());
        Assert.assertTrue(ce.hasNext());
        Assert.assertEquals("foo", ce.next());
        Assert.assertFalse(ce.hasNext());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NoSuchElementException.class)
    public void testNoSuchElementException() {
        final Iterator<String>[] enums = (Iterator<String>[]) new Iterator<?>[0];
        final Iterator<String> ce = Iterators.wrap(enums);
        ce.next();
    }
}

