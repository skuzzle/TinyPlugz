package de.skuzzle.tinyplugz.internal;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import de.skuzzle.tinyplugz.DeployListener;
import de.skuzzle.tinyplugz.Options;
import de.skuzzle.tinyplugz.PluginInformation;
import de.skuzzle.tinyplugz.PluginSource;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzException;
import de.skuzzle.tinyplugz.util.ElementIterator;

@RunWith(MockitoJUnitRunner.class)
public class TinyPlugzLookUpTest {

    static final class SampleTinyPlugzImpl extends TinyPlugz {

        public SampleTinyPlugzImpl() {}

        @Override
        protected void initialize(PluginSource source, ClassLoader parentClassLoader,
                Map<Object, Object> properties) throws TinyPlugzException {}

        @Override
        protected Iterator<DeployListener> findDeployListeners(
                ClassLoader pluginClassLoader) {
            return null;
        }

        @Override
        public Collection<PluginInformation> getPluginInformation() {
            return null;
        }

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
        public ElementIterator<URL> getResources(String name) throws IOException {
            return null;
        }

        @Override
        public <T> ElementIterator<T> getServices(Class<T> type) {
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

        @Override
        protected void dispose() {}
    }

    @Before
    public void setUp() throws Exception {}

    @Test
    public void testDefaultStrategy() throws Exception {
        final TinyPlugz inst = TinyPlugzLookUp.DEFAULT_INSTANCE_STRATEGY
                .getInstance(null, null);
        assertTrue(inst instanceof DefaultTinyPlugz);
    }

    @Test
    public void testStaticStrategyWithClassName() throws Exception {
        final String implName = SampleTinyPlugzImpl.class.getName();
        final Map<Object, Object> props = new HashMap<>();
        props.put(Options.FORCE_IMPLEMENTATION, implName);

        final TinyPlugz inst = TinyPlugzLookUp.STATIC_STRATEGY.getInstance(
                getClass().getClassLoader(), props);
        assertTrue(inst instanceof SampleTinyPlugzImpl);
    }

    @Test
    public void testStaticStrategyWithClass() throws Exception {
        final Map<Object, Object> props = new HashMap<>();
        props.put(Options.FORCE_IMPLEMENTATION, SampleTinyPlugzImpl.class);

        final TinyPlugz inst = TinyPlugzLookUp.STATIC_STRATEGY.getInstance(
                getClass().getClassLoader(), props);
        assertTrue(inst instanceof SampleTinyPlugzImpl);
    }

    @Test
    public void testStaticStrategyWithImpl() throws Exception {
        final TinyPlugz expected = new SampleTinyPlugzImpl();
        final Map<Object, Object> props = new HashMap<>();
        props.put(Options.FORCE_IMPLEMENTATION, expected);

        final TinyPlugz inst = TinyPlugzLookUp.STATIC_STRATEGY.getInstance(
                getClass().getClassLoader(), props);
        assertSame(expected, inst);
    }

    @Test(expected = TinyPlugzException.class)
    public void testStaticStrategyWrongType() throws Exception {
        final String implName = Object.class.getName();
        final Map<Object, Object> props = new HashMap<>();
        props.put(Options.FORCE_IMPLEMENTATION, implName);

        TinyPlugzLookUp.STATIC_STRATEGY.getInstance(getClass().getClassLoader(), props);
    }

    @Test
    public void testSPIStrategy() throws Exception {
        final TinyPlugz mock1 = mock(TinyPlugz.class);
        final TinyPlugz mock2 = mock(TinyPlugz.class);
        final ServiceLoaderWrapper loader = mock(ServiceLoaderWrapper.class);
        when(loader.loadService(TinyPlugz.class, getClass().getClassLoader()))
                .thenReturn(ElementIterator.wrap(Arrays.asList(mock1, mock2).iterator()));

        final TinyPlugz inst = TinyPlugzLookUp.SPI_STRATEGY
                .getInstance(getClass().getClassLoader(), loader, Collections.emptyMap());

        assertSame(mock1, inst);
    }

    @Test
    public void testSPIStrategyDefaultFallBack() throws Exception {
        final ServiceLoaderWrapper loader = mock(ServiceLoaderWrapper.class);
        when(loader.loadService(TinyPlugz.class, getClass().getClassLoader()))
                .thenReturn(ElementIterator.wrap(Collections.emptyIterator()));

        final TinyPlugz inst = TinyPlugzLookUp.SPI_STRATEGY
                .getInstance(getClass().getClassLoader(), null);
        assertTrue(inst instanceof DefaultTinyPlugz);
    }

    @Test(expected = TinyPlugzException.class)
    public void testSPIStrategyWithFail() throws Exception {
        final TinyPlugz mock1 = mock(TinyPlugz.class);
        final TinyPlugz mock2 = mock(TinyPlugz.class);
        final ServiceLoaderWrapper loader = mock(ServiceLoaderWrapper.class);
        when(loader.loadService(TinyPlugz.class, getClass().getClassLoader()))
                .thenReturn(ElementIterator.wrap(Arrays.asList(mock1, mock2).iterator()));

        final Map<Object, Object> props = new HashMap<>();
        props.put(Options.FAIL_ON_MULTIPLE_PROVIDERS, "true");

        TinyPlugzLookUp.SPI_STRATEGY.getInstance(getClass().getClassLoader(), loader, props);
    }
}
