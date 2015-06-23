package de.skuzzle.tinyplugz.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Files.class, PluginSourceBuilderImpl.class,
        PluginSourceBuilderImplTest.class })
public class PluginSourceBuilderImplTest {

    private PluginSourceBuilderImpl subject;

    // for creating distinct names
    private int pathCounter;

    @Before
    public void setUp() throws Exception {
        this.subject = new PluginSourceBuilderImpl();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddUnpackedNull() throws Exception {
        this.subject.addUnpackedPlugin(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddUnpackedNotADirectory() throws Exception {
        final Path path = mock(Path.class);
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.isDirectory(path)).thenReturn(false);
        this.subject.addUnpackedPlugin(path);
    }

    @Test
    public void testAddUnpackedPlugin() throws Exception {
        final Path path = mockPath();
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.isDirectory(path)).thenReturn(true);
        assertNotNull(this.subject.addUnpackedPlugin(path));
        assertEquals(1, this.subject.createSource().getPluginURLs().count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPluginjarNull() throws Exception {
        this.subject.addPluginJar(null);
    }

    @Test
    public void testAddPluginJar() throws Exception {
        final Path path = mockPath();
        assertNotNull(this.subject.addPluginJar(path));
        assertEquals(1, this.subject.createSource().getPluginURLs().count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPluginJarsNullFolder() throws Exception {
        this.subject.addAllPluginJars(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPluginJarsNullFilter() throws Exception {
        this.subject.addAllPluginJars(mock(Path.class), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddPluginJarsNotADirectory() throws Exception {
        final Path path = mock(Path.class);
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.isDirectory(path)).thenReturn(false);
        this.subject.addAllPluginJars(mock(Path.class), null);
    }

    private Path mockPath() throws MalformedURLException {
        final Path path = PowerMockito.mock(Path.class);
        final URI uri = PowerMockito.mock(URI.class);
        final URL url = PowerMockito.mock(URL.class);
        final String name = "url_" + this.pathCounter++;
        final Path fileName = PowerMockito.mock(Path.class);

        when(fileName.toString()).thenReturn(name);
        when(path.getFileName()).thenReturn(fileName);
        when(path.toUri()).thenReturn(uri);
        when(uri.toURL()).thenReturn(url);
        when(url.toString()).thenReturn(name);
        return path;
    }
}
