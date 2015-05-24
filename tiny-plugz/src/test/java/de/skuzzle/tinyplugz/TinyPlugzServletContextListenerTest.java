package de.skuzzle.tinyplugz;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DefineProperties;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator.DeployTinyPlugz;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TinyPlugzServletContextListener.class })
public class TinyPlugzServletContextListenerTest {

    @Mock
    private Path webInfDir;
    @Mock
    private ServletContextEvent contextEvent;
    @Mock
    private ServletContext context;
    @Mock
    private DeployTinyPlugz deployTinyPlugz;

    private TinyPlugzServletContextListener subject;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Paths.class);
        when(Paths.get("WEB-INF")).thenReturn(this.webInfDir);

        when(this.contextEvent.getServletContext()).thenReturn(this.context);
        when(this.context.getRealPath("WEB-INF")).thenReturn("WEB-INF");
    }

    @Test
    public void testInitContext() throws Exception {
        this.subject = new TinyPlugzServletContextListener() {
            @Override
            protected DeployTinyPlugz configure(DefineProperties props, Path webInfDir) {
                assertSame(TinyPlugzServletContextListenerTest.this.webInfDir, webInfDir);
                return TinyPlugzServletContextListenerTest.this.deployTinyPlugz;
            }
        };

        this.subject.contextInitialized(this.contextEvent);
    }
}
