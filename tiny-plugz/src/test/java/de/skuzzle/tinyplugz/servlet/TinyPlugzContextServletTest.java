package de.skuzzle.tinyplugz.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.skuzzle.tinyplugz.Options;
import de.skuzzle.tinyplugz.PluginSourceBuilder;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator;
import de.skuzzle.tinyplugz.servlet.TinyPlugzContextServlet;

@RunWith(MockitoJUnitRunner.class)
public class TinyPlugzContextServletTest {

    @Mock
    private Servlet mockServlet;
    @Mock
    private ServletRequest request;
    @Mock
    private ServletResponse response;
    @Mock
    private ServletConfig config;
    @Mock
    private TinyPlugz tinyPlugz;
    @Mock
    private ClassLoader classLoader;

    private Servlet servlet;
    private Servlet subject;

    @Before
    public void setUp() throws Exception {
        TinyPlugzConfigurator.setup()
            .withProperty(Options.FORCE_IMPLEMENTATION, this.tinyPlugz)
            .withPlugins(PluginSourceBuilder::noPlugins)
            .deploy();

        when(this.tinyPlugz.getClassLoader()).thenReturn(this.classLoader);
        when(this.mockServlet.getServletConfig()).thenReturn(this.config);
        when(this.mockServlet.getServletInfo()).thenReturn("info");
        this.servlet = new Servlet() {

            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException,
                    IOException {
                assertEquals(TinyPlugzContextServletTest.this.classLoader,
                        Thread.currentThread().getContextClassLoader());
            }

            @Override
            public void init(ServletConfig config) throws ServletException {
                assertEquals(TinyPlugzContextServletTest.this.classLoader,
                        Thread.currentThread().getContextClassLoader());
            }

            @Override
            public String getServletInfo() {
                return null;
            }

            @Override
            public ServletConfig getServletConfig() {
                return null;
            }

            @Override
            public void destroy() {
                assertEquals(TinyPlugzContextServletTest.this.classLoader,
                        Thread.currentThread().getContextClassLoader());
            }
        };
    }

    @After
    public void tearDown() {
        TinyPlugz.getInstance().undeploy();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrapNull() throws Exception {
        TinyPlugzContextServlet.wrap(null);
    }

    @Test
    public void testWrapContextServlet() throws Exception {
        this.subject = TinyPlugzContextServlet.wrap(this.mockServlet);
        final Servlet wrapAgain = TinyPlugzContextServlet.wrap(this.subject);
        assertSame(this.subject, wrapAgain);
    }

    @Test
    public void testInitCalled() throws Exception {
        this.subject = TinyPlugzContextServlet.wrap(this.mockServlet);
        this.subject.init(this.config);
        verify(this.mockServlet).init(this.config);
    }

    @Test
    public void testInitCorrectContext() throws Exception {
        this.subject = TinyPlugzContextServlet.wrap(this.servlet);
        this.subject.init(this.config);
    }

    @Test
    public void testServiceCalled() throws Exception {
        this.subject = TinyPlugzContextServlet.wrap(this.mockServlet);
        this.subject.service(this.request, this.response);
        verify(this.mockServlet).service(this.request, this.response);
    }

    @Test
    public void testServiceCorrectContext() throws Exception {
        this.subject = TinyPlugzContextServlet.wrap(this.servlet);
        this.subject.service(this.request, this.response);
    }

    @Test
    public void testDestroyCalled() throws Exception {
        this.subject = TinyPlugzContextServlet.wrap(this.mockServlet);
        this.subject.destroy();
        verify(this.mockServlet).destroy();
    }

    @Test
    public void testDestroyCorrectContext() throws Exception {
        this.subject = TinyPlugzContextServlet.wrap(this.servlet);
        this.subject.destroy();
    }

    @Test
    public void testGetConfigCalled() throws Exception {
        this.subject = TinyPlugzContextServlet.wrap(this.mockServlet);
        assertSame(this.config, this.subject.getServletConfig());
    }

    @Test
    public void testInfoCalled() throws Exception {
        this.subject = TinyPlugzContextServlet.wrap(this.mockServlet);
        assertEquals("info", this.subject.getServletInfo());
    }
}
