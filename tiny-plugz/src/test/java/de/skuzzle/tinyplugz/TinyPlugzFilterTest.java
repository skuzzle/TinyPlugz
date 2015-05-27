package de.skuzzle.tinyplugz;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.skuzzle.tinyplugz.test.util.MockUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MockUtil.class, TinyPlugzLookUp.class})
public class TinyPlugzFilterTest {

    @Mock
    private TinyPlugz tinyPlugz;
    @Mock
    private ClassLoader classLoaderMock;
    @Mock
    private ServletRequest request;
    @Mock
    private ServletResponse response;

    private Filter subject;

    @Before
    public void setUp() throws Exception {
        MockUtil.mockService(TinyPlugz.class, this.tinyPlugz);
        when(this.tinyPlugz.getClassLoader()).thenReturn(this.classLoaderMock);
        this.subject = new TinyPlugzFilter();
        TinyPlugzConfigurator.setup().withPlugins(PluginSource::noPlugins).deploy();
    }

    @After
    public void tearDown() {
        if (TinyPlugz.isDeployed()) {
            TinyPlugz.getInstance().undeploy();
        }
    }

    @Test
    public void testFilter() throws Exception {
        final FilterChain chain = new FilterChain() {

            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                assertSame(TinyPlugzFilterTest.this.classLoaderMock,
                        Thread.currentThread().getContextClassLoader());
            }
        };

        final ClassLoader realClassLoader = Thread.currentThread().getContextClassLoader();
        this.subject.doFilter(this.request, this.response, chain);
        assertSame(realClassLoader, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testFilterNoTinyPlugz() throws Exception {
        TinyPlugz.getInstance().undeploy();
        final FilterChain chain = mock(FilterChain.class);
        this.subject.doFilter(this.request, this.response, chain);
        verify(chain).doFilter(this.request, this.response);
    }

    @Test
    public void testFilterChain() throws Exception {
        final FilterChain chain = mock(FilterChain.class);
        this.subject.doFilter(this.request, this.response, chain);
        verify(chain).doFilter(this.request, this.response);
    }

    @Test
    public void testFilterAndRestoreOnException() throws Exception {
        final FilterChain chain = new FilterChain() {

            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                throw new IOException();
            }
        };
        final ClassLoader realClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            this.subject.doFilter(this.request, this.response, chain);
            fail();
        } catch (IOException e) {
            assertSame(realClassLoader, Thread.currentThread().getContextClassLoader());
        }
    }
}
