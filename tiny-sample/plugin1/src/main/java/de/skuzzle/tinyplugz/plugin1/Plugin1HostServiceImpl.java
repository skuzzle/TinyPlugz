package de.skuzzle.tinyplugz.plugin1;

import de.skuzzle.tinyplugz.test.HostService;

public class Plugin1HostServiceImpl implements HostService {

    @Override
    public String print(String s) {
        return "Plugin 1 says: " + s;
    }

}
