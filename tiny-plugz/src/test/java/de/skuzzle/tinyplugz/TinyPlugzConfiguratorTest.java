package de.skuzzle.tinyplugz;

import org.junit.Before;
import org.junit.Test;

public class TinyPlugzConfiguratorTest {

    private static void noPlugins(PluginSource source) {

    }

    @Before
    public void setUp() throws Exception {}

    @Test(expected = TinyPlugzException.class)
    public void testInvalidProperties() throws Exception {
        TinyPlugzConfigurator.setup()
                .withProperty(TinyPlugzConfigurator.FORCE_DEFAULT)
                .withProperty(TinyPlugzConfigurator.FORCE_IMPLEMENTATION, "foo.bar")
                .withPlugins(TinyPlugzConfiguratorTest::noPlugins)
                .deploy();
    }
}
