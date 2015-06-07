package de.skuzzle.tinyplugz;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.skuzzle.tinyplugz.util.ExchangeClassLoader;

@RunWith(MockitoJUnitRunner.class)
public class TinyPlugzFilterTest {

    @Mock
    private TinyPlugz tinyPlugz;
    @Mock
    private ClassLoader classLoaderMock;

    @Before
    public void setUp() throws Exception {
        when(this.tinyPlugz.getClassLoader()).thenReturn(this.classLoaderMock);
        TinyPlugzConfigurator.setup()
                .withProperty(Options.FORCE_IMPLEMENTATION, this.tinyPlugz)
                .withPlugins(PluginSource::noPlugins)
                .deploy();
    }

    @After
    public void tearDown() {
        if (TinyPlugz.isDeployed()) {
            TinyPlugz.getInstance().undeploy();
        }
    }

    @Test
    public void testExchange() throws Exception {
        final ClassLoader realClassLoader = Thread.currentThread().getContextClassLoader();

        try (ExchangeClassLoader exchange = ExchangeClassLoader.forTinyPlugz()) {
            assertSame(TinyPlugzFilterTest.this.classLoaderMock,
                    Thread.currentThread().getContextClassLoader());
        }

        assertSame(realClassLoader, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testExchangeNoTinyPlugz() throws Exception {
        TinyPlugz.getInstance().undeploy();
        try (ExchangeClassLoader exchange = ExchangeClassLoader.forTinyPlugz()) {
            // no exception caused be undeployed plugz
        }
    }

    @Test
    public void testExchangeAndRestoreOnException() throws Exception {
        final ClassLoader realClassLoader = Thread.currentThread().getContextClassLoader();
        try (ExchangeClassLoader exchange = ExchangeClassLoader.forTinyPlugz()) {
            throw new IOException();
        } catch (IOException e) {
        }
        assertSame(realClassLoader, Thread.currentThread().getContextClassLoader());
    }

    @Test(expected = IllegalStateException.class)
    public void testDiscoverExchange() throws Exception {
        try (ExchangeClassLoader exchange = ExchangeClassLoader.forTinyPlugz()) {
            exchange.setFailOnChange(true);
            final ClassLoader someClassLoader = mock(ClassLoader.class);
            Thread.currentThread().setContextClassLoader(someClassLoader);
        }
    }
}
