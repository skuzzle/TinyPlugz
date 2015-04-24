package de.skuzzle.tinyplugz.it;

import de.skuzzle.tinyplugz.HostSampleService;

public class HostSampleServiceImpl implements HostSampleService {

    @Override
    public String returnInput(String s) {
        return "Host " + s;
    }

}
