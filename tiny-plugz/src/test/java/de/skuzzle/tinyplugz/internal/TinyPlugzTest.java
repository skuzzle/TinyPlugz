package de.skuzzle.tinyplugz.internal;

import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import de.skuzzle.tinyplugz.AbstractTinyPlugzTest;
import de.skuzzle.tinyplugz.TinyPlugz;

@RunWith(MockitoJUnitRunner.class)
public class TinyPlugzTest extends AbstractTinyPlugzTest {

    private final TinyPlugz subject;

    public TinyPlugzTest() {
        this.subject = new DefaultTinyPlugz();
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
