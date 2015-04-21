package de.skuzzle.tinyplugz.guice;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;

import de.skuzzle.tinyplugz.AbstractTinyPlugzTest;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzException;
import de.skuzzle.tinyplugz.test.util.MockUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TinyPlugzGuice.class })
public class TinyPlugzGuiceTest extends AbstractTinyPlugzTest {

    private final TinyPlugzGuice subject;

    @SuppressWarnings("deprecation")
    public TinyPlugzGuiceTest() {
        this.subject = new TinyPlugzGuice();
    }

    @Override
    protected TinyPlugz getSubject() {
        return this.subject;
    }

    @Override
    public void setUp() throws TinyPlugzException {
        // override default setup
    }

    @SafeVarargs
    @Override
    protected final <T> void mockService(Class<T> service, T... impls) {
        final Module module = new AbstractModule() {

            @Override
            protected void configure() {
                final Multibinder<T> binder = Multibinder.newSetBinder(binder(), service);
                for (final T t : impls) {
                    binder.addBinding().toInstance(t);
                }
            }
        };
        MockUtil.mockService(Module.class, module);
        final ClassLoader mockCL = mock(ClassLoader.class);
        this.subject.initialize(Collections.emptySet(), mockCL, Collections.emptyMap());
    }

    @Test
    @Override
    public void testInitialized() throws Exception {
        mockService(Module.class);
        assertNotNull(this.subject.getClassLoader());
    }

    @Test
    public void loadServiceNoMultiBindings() throws Exception {
        final SampleService impl = mock(SampleService.class);
        final Module module = new AbstractModule() {

            @Override
            protected void configure() {
                bind(SampleService.class).toInstance(impl);
            }
        };
        MockUtil.mockService(Module.class, module);
        final ClassLoader mockCL = mock(ClassLoader.class);
        this.subject.initialize(Collections.emptySet(), mockCL, Collections.emptyMap());

        final Iterator<SampleService> provider = this.subject.getServices(SampleService.class);
        assertSame(impl, provider.next());
    }

    @Test
    public void testGetInjector() throws Exception {
        mockService(Module.class);
        final Injector injector = this.subject.getService(Injector.class);
        assertNotNull(injector);
    }

    @Test
    public void testGetDefaultBindings() throws Exception {
        mockService(Module.class);
        final TestDefaultInjections inst = this.subject.getService(TestDefaultInjections.class);
        assertSame(this.subject, inst.tinyPlugz);
        assertSame(this.subject.getClassLoader(), inst.classLoader);

    }

    private static class TestDefaultInjections {
        @Inject
        private TinyPlugz tinyPlugz;
        @Inject
        @Named(TinyPlugzGuice.PLUGIN_CLASSLOADER)
        private ClassLoader classLoader;
    }
}
