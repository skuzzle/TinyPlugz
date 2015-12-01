package de.skuzzle.tinyplugz.guice;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Guice.class })
public class DefaultInjectorFactoryTest {

    private final InjectorFactory subject = new DefaultInjectorFactory();

    @Before
    public void setUp() throws Exception {}

    @Test
    public void testWithParent() throws Exception {
        final Injector injector = mock(Injector.class);
        final Injector child = mock(Injector.class);
        when(injector.createChildInjector(Collections.emptyList())).thenReturn(child);
        final Map<Object, Object> props = new HashMap<>();
        props.put(TinyPlugzGuice.PARENT_INJECTOR, injector);
        final Injector result = this.subject.createInjector(Collections.emptyList(), props);
        assertSame(child, result);
    }

    @Test
    public void testWithoutParent() throws Exception {
        final Injector injector = mock(Injector.class);
        PowerMockito.mockStatic(Guice.class);
        PowerMockito.when(Guice.createInjector(Stage.PRODUCTION, Collections.emptyList())).thenReturn(injector);
        final Injector result = this.subject.createInjector(Collections.emptyList(), Collections.emptyMap());
        assertSame(injector, result);
    }

    @Test
    public void testWithoutParentAndStage() throws Exception {
        final Injector injector = mock(Injector.class);
        final Map<Object, Object> props = new HashMap<>();
        props.put(TinyPlugzGuice.INJECTOR_STAGE, "DEVELOPMENT");
        PowerMockito.mockStatic(Guice.class);
        PowerMockito.when(Guice.createInjector(Stage.DEVELOPMENT, Collections.emptyList())).thenReturn(injector);
        final Injector result = this.subject.createInjector(Collections.emptyList(), props);
        assertSame(injector, result);
    }
}
