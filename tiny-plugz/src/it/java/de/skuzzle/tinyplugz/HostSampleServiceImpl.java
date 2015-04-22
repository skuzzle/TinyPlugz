package de.skuzzle.tinyplugz;

public class HostSampleServiceImpl implements HostSampleService {

    @Override
    public String returnInput(String s) {
        return "Host " + s;
    }

}
