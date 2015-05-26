package de.skuzzle.tinyplugz.guice;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Injector;

import de.skuzzle.tinyplugz.PluginSource;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DefineProperties;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DeployTinyPlugz;

@RunWith(MockitoJUnitRunner.class)
public class TinyPlugzGuiceServletContextListenerTest {

    @Mock
    private TinyPlugz tinyPlugz;
    @Mock
    private Injector injector;
    @Mock
    private ServletContext context;
    @Mock
    private ServletContextEvent contextEvent;
    @Mock
    private Path webInfDir;

    private TinyPlugzGuiceServletContextListener subject;

    @Before
    public void setUp() throws Exception {
        when(this.contextEvent.getServletContext()).thenReturn(this.context);
        when(this.context.getRealPath("WEB-INF")).thenReturn("WEB-INF");

        this.subject = new TinyPlugzGuiceServletContextListener() {

            @Override
            protected DeployTinyPlugz configure(DefineProperties props,
                    ServletContext context) {
                return props.withPlugins(PluginSource::noPlugins);
            }
        };
    }

    @Test
    public void testContextInitialized() throws Exception {
        this.subject.contextInitialized(this.contextEvent);
        assertNotNull(this.subject.getInjector());
    }

    @Test(expected = IllegalStateException.class)
    public void testContextDestroyed() throws Exception {
        this.subject.contextInitialized(this.contextEvent);
        this.subject.contextDestroyed(this.contextEvent);
        this.subject.getInjector();
    }
}
