package de.skuzzle.tinyplugz.it;

public class HostSampleServiceImpl implements HostSampleService {

    @Override
    public String returnInput(String s) {
        return "Host " + s;
    }

}
