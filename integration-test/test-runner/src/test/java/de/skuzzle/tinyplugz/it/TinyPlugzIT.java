package de.skuzzle.tinyplugz.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.skuzzle.tinyplugz.HostSampleService;
import de.skuzzle.tinyplugz.PluginInformation;
import de.skuzzle.tinyplugz.PluginSourceBuilder;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator;
import de.skuzzle.tinyplugz.TinyPlugzException;

public class TinyPlugzIT {

    public static final boolean IS_MAVEN = Boolean.parseBoolean(
            System.getProperty("isMaven", "false"));

    @Before
    public void setup() throws TinyPlugzException {
        TinyPlugzConfigurator.setup()
                .withPlugins(TinyPlugzIT::selectPlugins)
                .deploy();
    }

    @After
    public void tearDown() {
        TinyPlugz.getInstance().undeploy();
    }

    private static void selectPlugins(PluginSourceBuilder source) {
        source.addUnpackedPlugin(plugin("test-plugin1"));
        source.addUnpackedPlugin(plugin("test-plugin2"));
    }

    private static Path plugin(String name) {
        final Path base = new File(".").getAbsoluteFile().toPath();
        return base.getParent().getParent().resolve(name)
                .resolve("target/test-classes/");
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testGetPluginInformation() throws Exception {
        final PluginInformation info1 = TinyPlugz.getInstance()
                .getPluginInformation("plugin1")
                .get();
        final PluginInformation info2 = TinyPlugz.getInstance()
                .getPluginInformation("plugin2")
                .get();

        assertEquals("plugin1", info1.getName());
        assertEquals("plugin2", info2.getName());
    }

    @Test
    public void testGetServiceFailMultiple() throws Exception {
        this.exception.expect(IllegalStateException.class);
        this.exception.expectMessage("multiple providers");
        TinyPlugz.getInstance().getService(HostSampleService.class);
    }

    @Test
    public void testGetServices() throws Exception {
        final Set<String> expected = new HashSet<>(
                Arrays.asList("Host foo", "Plugin1 foo", "Plugin2 foo"));

        final Iterator<HostSampleService> it = TinyPlugz.getInstance()
                .getServices(HostSampleService.class);
        final Set<String> actual = new HashSet<>();
        it.forEachRemaining(service -> actual.add(service.returnInput("foo")));
        assertEquals(expected, actual);
    }

    @Test
    public void testGetFirstService() throws Exception {
        final Optional<HostSampleService> opt = TinyPlugz.getInstance()
                .getFirstService(HostSampleService.class);
        assertTrue(opt.isPresent());
    }

    @Test
    public void testTryLoadDependencyClass() throws Exception {
        this.exception.expect(ClassNotFoundException.class);
        this.exception.expectMessage("de.skuzzle.semantic.Version");
        TinyPlugz.getInstance().getClassLoader().loadClass("de.skuzzle.semantic.Version");
    }

    @Test
    public void testGetResources() throws Exception {
        final Iterator<URL> it = TinyPlugz.getInstance().getResources("both.txt");
        it.next();
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    public void testGetResource() throws Exception {
        final Optional<URL> plugin1 = TinyPlugz.getInstance().getResource("plugin1.txt");
        final Optional<URL> plugin2 = TinyPlugz.getInstance().getResource("plugin2.txt");
        assertTrue(plugin1.isPresent());
        assertTrue(plugin2.isPresent());
    }

    @Test
    public void testClassLoaderHierarchy() throws Exception {
        TinyPlugz.getInstance().getClassLoader().loadClass(
                "de.skuzzle.tinyplugz.test.testplugin1.ClassWithPlugin1Dependency");
    }
}
