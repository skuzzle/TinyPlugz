package de.skuzzle.tinyplugz.test.testplugin1;
import de.skuzzle.tinyplugz.HostSampleService;

public class Plugin1HostInterfaceImpl implements HostSampleService {

    @Override
    public String returnInput(String s) {
        return "Plugin1 " + s;
    }

}
