package de.skuzzle.tinyplugz;

import org.junit.Before;
import org.junit.Test;

public class TinyPlugzConfiguratorTest {

    @Before
    public void setUp() throws Exception {}

    @Test(expected = TinyPlugzException.class)
    public void testInvalidProperties() throws Exception {
        TinyPlugzConfigurator.setup()
                .withProperty(Options.FORCE_DEFAULT)
                .withProperty(Options.FORCE_IMPLEMENTATION, "foo.bar")
                .withPlugins(PluginSource::noPlugins)
                .deploy();
    }
}
