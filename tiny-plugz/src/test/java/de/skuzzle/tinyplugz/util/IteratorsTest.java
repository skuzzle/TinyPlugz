package de.skuzzle.tinyplugz.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;

public class IteratorsTest {

    @Test
    public void testFilter() throws Exception {
        final Predicate<String> pred = s -> s.startsWith("a");
        final Collection<String> sample = Arrays.asList("a", "b", "a");
        final Iterator<String> filtered = Iterators.filter(sample.iterator(), pred);
        assertEquals("a", filtered.next());
        assertEquals("a", filtered.next());
        assertFalse(filtered.hasNext());
    }

    @Test
    public void testFilterMultipleHasNextCalls() throws Exception {
        final Predicate<String> pred = s -> s.startsWith("a");
        final Collection<String> sample = Arrays.asList("a", "b", "a");
        final Iterator<String> filtered = Iterators.filter(sample.iterator(), pred);
        assertTrue(filtered.hasNext());
        assertTrue(filtered.hasNext());
        assertTrue("has next must not advance the original iterator", filtered.hasNext());
    }

    @Test(expected = RuntimeException.class)
    public void testFilterEmptyRemove() throws Exception {
        final Predicate<String> pred = s -> s.startsWith("a");
        final Collection<String> sample = Arrays.asList();
        final Iterator<String> filtered = Iterators.filter(sample.iterator(), pred);
        filtered.remove();
    }

    @Test
    public void testFilterRemove() throws Exception {
        final Predicate<String> pred = s -> s.startsWith("a");
        final Collection<String> sample = new ArrayList<>(Arrays.asList("a"));
        final Iterator<String> filtered = Iterators.filter(sample.iterator(), pred);
        filtered.next();
        filtered.remove();
        assertTrue(sample.isEmpty());
    }

    @Test
    public void testFilterNoMatches() throws Exception {
        final Predicate<String> pred = s -> s.startsWith("a");
        final Collection<String> sample = Arrays.asList("b", "b", "c");
        final Iterator<String> filtered = Iterators.filter(sample.iterator(), pred);
        assertFalse(filtered.hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void testFilterNoMatchesException() throws Exception {
        final Predicate<String> pred = s -> s.startsWith("a");
        final Collection<String> sample = Arrays.asList("b", "b", "c");
        final Iterator<String> filtered = Iterators.filter(sample.iterator(), pred);
        filtered.next();
    }

    @Test
    public void testFilterEmpty() throws Exception {
        final Predicate<String> pred = s -> s.startsWith("a");
        final Collection<String> sample = Arrays.asList();
        final Iterator<String> filtered = Iterators.filter(sample.iterator(), pred);
        assertFalse(filtered.hasNext());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testCompositeNullIterator() {
        Iterators.compositeIterator((Iterator[]) null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testCompositeNullIterable() {
        Iterators.compositeIterable((Iterable[]) null);
    }

    @Test
    public void testCompositeIterableEmpty() throws Exception {
        final Iterable<String> wrapped = Iterators.compositeIterable();
        assertFalse(wrapped.iterator().hasNext());
    }

    @Test
    public void testCompositeIterables() throws Exception {
        final Collection<String> first = Arrays.asList("a", "b");
        final Collection<String> second = Arrays.asList("c", "d");
        final Iterable<String> wrapped = Iterators.compositeIterable(first, second);
        final Iterator<String> it = wrapped.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertEquals("d", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testCompositeSingleIterable() throws Exception {
        final Iterable<String> it = Collections.emptyList();
        assertSame(it, Iterators.compositeIterable(it));
    }

    @Test
    public void testCompositeSingleIterator() throws Exception {
        final Iterator<String> it = Arrays.asList("a").iterator();
        final ElementIterator<String> wrapped = Iterators.compositeIterator(it);
        assertEquals("a", wrapped.next());
        assertFalse(wrapped.hasNext());
    }

    @Test
    public void testCompositeSingleElementIterator() throws Exception {
        final Iterator<String> it = Arrays.asList("a").iterator();
        final ElementIterator<String> wrapped = ElementIterator.wrap(it);
        final ElementIterator<String> composite = Iterators.compositeIterator(wrapped);
        assertSame(wrapped, composite);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAllEmpty() {
        final Collection<String> v = new ArrayList<>();
        final Iterator<String>[] enums = (Iterator<String>[]) new Iterator<?>[] { v
                .iterator(), v.iterator(), v.iterator() };
        final Iterator<String> ce = Iterators.compositeIterator(enums);
        Assert.assertFalse(ce.hasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmptyArray() {
        final Iterator<String>[] enums = (Iterator<String>[]) new Iterator<?>[0];
        final Iterator<String> ce = Iterators.compositeIterator(enums);
        Assert.assertFalse(ce.hasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMiddleIsEmpty() {
        final Collection<String> empty = new ArrayList<>();
        final Collection<String> oneElement = Arrays.asList("foo");
        final Iterator<String>[] enums = (Iterator<String>[]) new Iterator<?>[] {
                oneElement.iterator(), empty.iterator(),
                oneElement.iterator() };
        final Iterator<String> ce = Iterators.compositeIterator(enums);
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
        final Iterator<String>[] enums = (Iterator<String>[]) new Iterator<?>[] { empty
                .iterator(), oneElement.iterator(),
                oneElement.iterator() };
        final Iterator<String> ce = Iterators.compositeIterator(enums);
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
        final Iterator<String>[] enums = (Iterator<String>[]) new Iterator<?>[] {
                oneElement.iterator(), oneElement.iterator(),
                empty.iterator() };
        final Iterator<String> ce = Iterators.compositeIterator(enums);
        Assert.assertEquals("foo", ce.next());
        Assert.assertTrue(ce.hasNext());
        Assert.assertEquals("foo", ce.next());
        Assert.assertFalse(ce.hasNext());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NoSuchElementException.class)
    public void testNoSuchElementException() {
        final Iterator<String>[] enums = (Iterator<String>[]) new Iterator<?>[0];
        final Iterator<String> ce = Iterators.compositeIterator(enums);
        ce.next();
    }
}
