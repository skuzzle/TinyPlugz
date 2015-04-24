package de.skuzzle.tinyplugz.test.testplugin2;
import de.skuzzle.tinyplugz.TinyPlugz;
import de.skuzzle.tinyplugz.it.HostSampleService;
import de.skuzzle.tinyplugz.test.testplugin1.Plugin1SampleService;

public class Plugin1HostInterfaceImpl implements HostSampleService {

    @Override
    public String returnInput(String s) {
        final Plugin1SampleService plugin1Service = TinyPlugz.getInstance()
                .getService(Plugin1SampleService.class);
        return "Plugin2 " + plugin1Service.workit(s);
    }

}
