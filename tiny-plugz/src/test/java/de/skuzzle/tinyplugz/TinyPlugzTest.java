package de.skuzzle.tinyplugz;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.skuzzle.tinyplugz.TinyPlugzConfigurator.TinyPlugzImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TinyPlugz.class })
public class TinyPlugzTest extends AbstractTinyPlugzTest {

    private final TinyPlugzImpl subject;

    @SuppressWarnings("deprecation")
    public TinyPlugzTest() {
        this.subject = new TinyPlugzImpl();
    }

    @Override
    protected final TinyPlugz getSubject() {
        return this.subject;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> void mockService(Class<T> service, T... impls) {
        defaultMockService(service, impls);
    }
}
