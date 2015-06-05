package de.skuzzle.tinyplugz.test.testplugin2;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import de.skuzzle.semantic.Version;
import de.skuzzle.tinyplugz.HostSampleService;
import de.skuzzle.tinyplugz.Require;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.test.testplugin1.Plugin1SampleService;

public class Plugin1HostInterfaceImpl implements HostSampleService {

    public Plugin1HostInterfaceImpl() throws ClassNotFoundException, IOException {
        // check if we can access dependency
        Version.create(1, 2, 3);
        // check if we can access class from plugin1
        final ClassLoader cl = getClass().getClassLoader();
        final Class<?> cls = cl.loadClass(
                "de.skuzzle.tinyplugz.test.testplugin1.Plugin1SampleService");
        Require.condition(!cls.getClassLoader().equals(cl),
                "Class '%s' loaded with wrong classloader", cls.getName());

        // check if we can access own resource
        final URL plugin1 = cl.getResource("plugin1.txt");
        final URL plugin2 = cl.getResource("plugin2.txt");
        final Enumeration<URL> both = cl.getResources("both.txt");

        Require.nonNull(plugin1, "plugin1");
        Require.nonNull(plugin2, "plugin1");
        both.nextElement();
        both.nextElement();
        Require.condition(!both.hasMoreElements(), "");
    }

    @Override
    public String returnInput(String s) {
        final Plugin1SampleService plugin1Service = TinyPlugz.getInstance()
                .getService(Plugin1SampleService.class);
        return "Plugin2 " + plugin1Service.workit(s);
    }

}
