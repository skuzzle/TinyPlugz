package de.skuzzle.tinyplugz;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
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
     * No-Op {@link PluginSource} consumer which does not add any plugins.
     *
     * @param source The plugin source.
     */
    protected static void noPlugins(PluginSource source) {}

    protected AbstractTinyPlugzTest() {

    }

    @Before
    public void setUp() throws TinyPlugzException {
        final ClassLoader mockCL = mock(ClassLoader.class);
        getSubject().initialize(Collections.emptySet(), mockCL, Collections.emptyMap());
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
    public void testRunInContextClassLoader() throws Exception {
        final ClassLoader backup = Thread.currentThread().getContextClassLoader();
        getSubject().contextClassLoaderScope(() -> {
            final ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
            assertNotSame(contextCl, backup);
        });
        assertSame(backup, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testRunInContextClassLoaderRestoreOnException() throws Exception {
        final ClassLoader backup = Thread.currentThread().getContextClassLoader();
        try {
            getSubject().contextClassLoaderScope(() -> {
                throw new RuntimeException();
            });
            fail();
        } catch (RuntimeException e) {
            assertSame(backup, Thread.currentThread().getContextClassLoader());
        }
    }

}