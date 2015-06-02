package de.skuzzle.tinyplugz.test.testplugin2;

import de.skuzzle.semantic.Version;
import de.skuzzle.tinyplugz.HostSampleService;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.test.testplugin1.Plugin1SampleService;

public class Plugin1HostInterfaceImpl implements HostSampleService {

    public Plugin1HostInterfaceImpl() {
        // check if we can access dependency
        System.out.println(getClass().getClassLoader());
        Version.create(1, 2, 3);
    }

    @Override
    public String returnInput(String s) {
        final Plugin1SampleService plugin1Service = TinyPlugz.getInstance()
                .getService(Plugin1SampleService.class);
        return "Plugin2 " + plugin1Service.workit(s);
    }

}
