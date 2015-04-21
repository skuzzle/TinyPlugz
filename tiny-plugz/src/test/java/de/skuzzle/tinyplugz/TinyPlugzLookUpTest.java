package de.skuzzle.tinyplugz;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.skuzzle.tinyplugz.TinyPlugzConfigurator.TinyPlugzImpl;
import de.skuzzle.tinyplugz.test.util.MockUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ServiceLoader.class, MockUtil.class, TinyPlugzLookUp.class })
public class TinyPlugzLookUpTest {

    static final class SampleTinyPlugzImpl extends TinyPlugz {

        @Override
        protected void initialize(Collection<URL> urls, ClassLoader parentClassLoader,
                Map<Object, Object> properties) throws TinyPlugzException {}

        @Override
        public void runMain(String className, String[] args) throws TinyPlugzException {}

        @Override
        public ClassLoader getClassLoader() {
            return null;
        }

        @Override
        public Optional<URL> getResource(String name) {
            return null;
        }

        @Override
        public Iterator<URL> getResources(String name) throws IOException {
            return null;
        }

        @Override
        public void contextClassLoaderScope(Runnable r) {}

        @Override
        public <T> Iterator<T> getServices(Class<T> type) {
            return null;
        }

        @Override
        public <T> Optional<T> getFirstService(Class<T> type) {
            return null;
        }

        @Override
        public <T> T getService(Class<T> type) {
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {}

    @Test
    public void testDefaultStrategy() throws Exception {
        final TinyPlugz inst = TinyPlugzLookUp.DEFAULT_INSTANCE_STRATEGY
                .getInstance(null, null);
        assertTrue(inst instanceof TinyPlugzImpl);
    }

    @Test
    public void testStaticStrategy() throws Exception {
        final String implName = SampleTinyPlugzImpl.class.getName();
        final Map<Object, Object> props = new HashMap<>();
        props.put(TinyPlugzConfigurator.FORCE_IMPLEMENTATION, implName);

        final TinyPlugz inst = TinyPlugzLookUp.STATIC_STRATEGY.getInstance(
                getClass().getClassLoader(), props);
        assertTrue(inst instanceof SampleTinyPlugzImpl);
    }

    @Test(expected = TinyPlugzException.class)
    public void testStaticStrategyWrongType() throws Exception {
        final String implName = Object.class.getName();
        final Map<Object, Object> props = new HashMap<>();
        props.put(TinyPlugzConfigurator.FORCE_IMPLEMENTATION, implName);

        TinyPlugzLookUp.STATIC_STRATEGY.getInstance(getClass().getClassLoader(), props);
    }

    @Test
    public void testSPIStrategy() throws Exception {
        final TinyPlugz mock1 = mock(TinyPlugz.class);
        final TinyPlugz mock2 = mock(TinyPlugz.class);
        MockUtil.mockService(TinyPlugz.class, mock1, mock2);

        final TinyPlugz inst = TinyPlugzLookUp.SPI_STRATEGY
                .getInstance(getClass().getClassLoader(), null);

        assertSame(mock1, inst);
    }

    @Test
    public void testSPIStrategyDefaultFallBack() throws Exception {
        MockUtil.mockService(TinyPlugz.class);

        final TinyPlugz inst = TinyPlugzLookUp.SPI_STRATEGY
                .getInstance(getClass().getClassLoader(), null);
        assertTrue(inst instanceof TinyPlugzImpl);
    }
}
