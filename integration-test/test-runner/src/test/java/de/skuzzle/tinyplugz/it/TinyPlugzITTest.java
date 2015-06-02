package de.skuzzle.tinyplugz.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
import de.skuzzle.tinyplugz.PluginSource;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.TinyPlugzConfigurator;
import de.skuzzle.tinyplugz.TinyPlugzException;

public class TinyPlugzITTest {

    public static final boolean IS_MAVEN = Boolean.parseBoolean(
            System.getProperty("isMaven", "false"));

    @Before
    public void setup() throws TinyPlugzException {
        TinyPlugzConfigurator.setup()
                .withPlugins(TinyPlugzITTest::selectPlugins)
                .deploy();
    }

    @After
    public void tearDown() {
        TinyPlugz.getInstance().undeploy();
    }

    private static void selectPlugins(PluginSource source) {
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

    @Test(expected = ClassNotFoundException.class)
    public void testTryLoadDependencyClass() throws Exception {
        TinyPlugz.getInstance().getClassLoader().loadClass("de.skuzzle.semantic.Version");
    }
}
