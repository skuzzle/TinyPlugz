package de.skuzzle.tinyplugz.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class DelegateDependencyResolverTest {

    private URL url;

    @Mock
    private DependencyResolver requestor;
    @Mock
    private DependencyResolver delegate1;
    @Mock
    private DependencyResolver delegate2;

    private DelegateDependencyResolver subject;

    @Before
    public void setUp() throws Exception {
        final Collection<DependencyResolver> delegates = Arrays.asList(
                this.requestor, this.delegate1, this.delegate2);
        this.subject = new DelegateDependencyResolver(delegates);

        // arbitrary non-null value
        this.url = new URL("http://www.google.de");
    }

    @Test
    public void testFindClassRequestorNull() throws Exception {
        // null as requestor must be explicitly allowed
        final Class<?> cls = this.subject.findClass(null, "de.skuzzle.TestClass");
        assertNull(cls);
    }

    @Test
    public void testFindClass() throws Exception {
        final String name = "de.skuzzle.TestClass";
        final Class result = getClass();

        when(this.delegate2.findClass(this.requestor, name)).thenReturn(result);
        final Class<?> find = this.subject.findClass(this.requestor, name);
        assertSame(result, find);
        verify(this.requestor, never()).findClass(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testFindClassByIndex() throws Exception {
        final String name = "de.skuzzle.TestClass";
        final Class result = getClass();

        when(this.delegate2.findClass(this.requestor, name)).thenReturn(result);
        final Class<?> find1 = this.subject.findClass(this.requestor, name);
        final Class<?> find2 = this.subject.findClass(this.requestor, name);

        assertSame(result, find1);
        assertSame(result, find2);

        verify(this.delegate1, times(1)).findClass(Mockito.any(), Mockito.anyString());
        verify(this.requestor, never()).findClass(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testFindByClassIndexSkipRequestor() throws Exception {
        final String name = "de.skuzzle.TestClass";
        final Class result = getClass();

        when(this.delegate2.findClass(this.requestor, name)).thenReturn(result);
        final Class<?> find1 = this.subject.findClass(this.requestor, name);
        final Class<?> find2 = this.subject.findClass(this.delegate2, name);

        assertSame(result, find1);
        assertNull(find2);

        verify(this.delegate2, times(1)).findClass(Mockito.any(), Mockito.anyString());
        verify(this.requestor, times(1)).findClass(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testFindResource() throws Exception {
        when(this.delegate2.findResource(this.requestor, "foo")).thenReturn(this.url);

        final URL find = this.subject.findResource(this.requestor, "foo");

        assertSame(this.url, find);
        verify(this.requestor, never()).findResource(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testFindResources() throws Exception {
        final Collection<URL> target = new ArrayList<>();

        stubFindResources(target, this.delegate1);
        stubFindResources(target, this.delegate2);

        this.subject.findResources(this.requestor, "foo", target);

        assertEquals(2, target.size());
    }

    private void stubFindResources(Collection<URL> target, DependencyResolver resolver) throws IOException {
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final Collection<URL> c = invocation.getArgumentAt(2, Collection.class);
                c.add(DelegateDependencyResolverTest.this.url);
                return null;
            }}).when(resolver).findResources(this.requestor, "foo", target);
    }

    @Test
    public void testClose() throws Exception {
        this.subject.close();

        verify(this.requestor).close();
        verify(this.delegate1).close();
        verify(this.delegate2).close();
    }
}
