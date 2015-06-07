package de.skuzzle.tinyplugz;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.skuzzle.tinyplugz.test.util.MockUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ServiceLoader.class, MockUtil.class })
public abstract class AbstractTinyPlugzTest {

    public static interface SampleService {
        public void callMe();
    }

    /**
     * No-Op {@link PluginSourceBuilder} consumer which does not add any plugins.
     *
     * @param source The plugin source.
     */
    protected static void noPlugins(PluginSourceBuilder source) {}

    protected AbstractTinyPlugzTest() {

    }

    @Before
    public void setUp() throws TinyPlugzException {
        final ClassLoader parent = getClass().getClassLoader();
        getSubject().initialize(PluginSource.empty(), parent, Collections.emptyMap());
    }

    protected abstract TinyPlugz getSubject();

    @SuppressWarnings("unchecked")
    protected abstract <T> void mockService(Class<T> service, T... impls);

    @SafeVarargs
    protected final <T> void defaultMockService(Class<T> service, T... impls) {
        MockUtil.mockService(service, impls);
    }

    @Test
    public void testInitialized() throws Exception {
        assertNotNull(getSubject().getClassLoader());
    }

    @Test
    public void testGetServices() throws Exception {
        final SampleService impl1 = mock(SampleService.class);
        final SampleService impl2 = mock(SampleService.class);
        mockService(SampleService.class, impl1, impl2);

        final Iterator<SampleService> it = getSubject()
                .getServices(SampleService.class);
        assertSame(impl1, it.next());
        assertSame(impl2, it.next());
    }

    @Test
    public void testGetServicesEmpty() throws Exception {
        mockService(SampleService.class);
        final Iterator<SampleService> it = getSubject()
                .getServices(SampleService.class);
        assertFalse(it.hasNext());
    }

    @Test
    public void testGetFirstServiceEmpty() throws Exception {
        mockService(SampleService.class);

        final Optional<SampleService> it = getSubject().getFirstService(SampleService.class);
        assertFalse(it.isPresent());
    }

    @Test
    public void testGetFirstService() throws Exception {
        final SampleService impl1 = mock(SampleService.class);
        final SampleService impl2 = mock(SampleService.class);
        mockService(SampleService.class, impl1, impl2);

        final Optional<SampleService> it = getSubject().getFirstService(SampleService.class);
        assertSame(impl1, it.get());
    }

    @Test
    public void testIsServiceAvailableNoService() throws Exception {
        mockService(SampleService.class);
        assertFalse(getSubject().isServiceAvailable(SampleService.class));
    }

    @Test
    public void testIsServiceAvailable() throws Exception {
        final SampleService impl1 = mock(SampleService.class);
        mockService(SampleService.class, impl1);
        assertTrue(getSubject().isServiceAvailable(SampleService.class));
    }

    @Test(expected = RuntimeException.class)
    public void testGetServiceMultipleServices() throws Exception {
        final SampleService impl1 = mock(SampleService.class);
        final SampleService impl2 = mock(SampleService.class);
        mockService(SampleService.class, impl1, impl2);

        getSubject().getService(SampleService.class);
    }

    @Test(expected = RuntimeException.class)
    public void testGetServiceNoServices() throws Exception {
        mockService(SampleService.class);
        getSubject().getService(SampleService.class);
    }

    @Test
    public void testGetService() throws Exception {
        final SampleService impl1 = mock(SampleService.class);
        mockService(SampleService.class, impl1);

        final SampleService service = getSubject().getService(SampleService.class);
        assertSame(impl1, service);
    }

    @Test
    public void testRunMain() throws Exception {
        final String name = "de.skuzzle.tinyplugz.Main";
        getSubject().runMain(name, new String[0]);
        assertTrue(Main.called);
    }
}