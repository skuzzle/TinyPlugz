package de.skuzzle.tinyplugz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import de.skuzzle.tinyplugz.test.HostService;

public class DefaultTinyPlugzTest {

    @Before
    public void setup() throws TinyPlugzException {
        TinyPlugzConfigurator.setup()
                .withPlugins(source -> source
                        .addUnpackedPlugin(folderOf("plugin1"))
                        .addUnpackedPlugin(folderOf("plugin2")))
                .deploy();
    }

    private Path folderOf(String name) {
        final Path me = new File(".").getAbsoluteFile().toPath();
        return me.getParent().getParent().resolve(name).resolve("target/classes");
    }

    @Test
    public void testGetHostService() throws Exception {
        final Iterator<HostService> providers = TinyPlugz.getDefault()
                .getServices(HostService.class);
        assertTrue(providers.hasNext());
        final HostService service = providers.next();
        final String expected = "Plugin 1 says: foo";
        assertEquals(expected, service.print("foo"));
    }
}
