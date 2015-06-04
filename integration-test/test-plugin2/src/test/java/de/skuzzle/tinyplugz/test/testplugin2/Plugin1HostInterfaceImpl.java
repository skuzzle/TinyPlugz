package de.skuzzle.tinyplugz.test.testplugin2;

import de.skuzzle.semantic.Version;
import de.skuzzle.tinyplugz.HostSampleService;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.test.testplugin1.Plugin1SampleService;

public class Plugin1HostInterfaceImpl implements HostSampleService {

    public Plugin1HostInterfaceImpl() throws ClassNotFoundException {
        // check if we can access dependency
        Version.create(1, 2, 3);
        // check if we can access class from plugin1
        getClass().getClassLoader().loadClass(
                "de.skuzzle.tinyplugz.test.testplugin1.Plugin1SampleService");
    }

    @Override
    public String returnInput(String s) {
        final Plugin1SampleService plugin1Service = TinyPlugz.getInstance()
                .getService(Plugin1SampleService.class);
        return "Plugin2 " + plugin1Service.workit(s);
    }

}
