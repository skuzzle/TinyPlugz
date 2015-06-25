package de.skuzzle.tinyplugz.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloseablesTest {

    @Mock
    private Closeable failing;
    @Mock
    private Closeable successful;

    @Before
    public void setUp() throws Exception {
        doThrow(IOException.class).when(this.failing).close();
    }

    @Test
    public void testSafeCloseSuccessful() throws Exception {
        assertTrue(Closeables.safeClose(this.successful));
        verify(this.successful).close();
    }

    @Test
    public void testSafeCloseFailing() throws Exception {
        assertFalse(Closeables.safeClose(this.failing));
        verify(this.failing).close();
    }

    @Test
    public void testSafeCloseNull() throws Exception {
        Closeables.safeClose(null);
    }

    @Test
    public void testSafeCloseMethodReference() throws Exception {
        assertFalse(Closeables.safeClose(this.failing::close));
    }

    @Test
    public void testSafeCloseAll() throws Exception {
        final List<Closeable> all = Arrays.asList(this.failing, null, this.successful);
        assertFalse(Closeables.safeCloseAll(all));
        verify(this.failing).close();
        verify(this.successful).close();
    }

    @Test
    public void testSafeCloseAllSuccessful() throws Exception {
        final List<Closeable> all = Arrays.asList(this.successful);
        assertTrue(Closeables.safeCloseAll(all));
        verify(this.successful).close();
    }

    @Test
    public void testSafeCloseAllArray() throws Exception {
        assertFalse(Closeables.safeCloseAll(this.failing, null, this.successful));
        verify(this.failing).close();
        verify(this.successful).close();
    }
}
