package de.skuzzle.tinyplugz.test.util;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.powermock.api.mockito.PowerMockito;

public final class MockUtil {

    private MockUtil() {}

    /**
     * Mocks a {@link ServiceLoader} to return an Iterator of the given
     * implementations for the given service interface.
     *
     * @param service The service interface.
     * @param impls Implementations of that interface.
     */
    @SuppressWarnings("unchecked")
    public static <T> void mockService(Class<T> service, T... impls) {
        final ServiceLoader<T> fakeService = mock(ServiceLoader.class);
        final Iterator<T> implIt = Arrays.stream(impls).iterator();
        when(fakeService.iterator()).thenReturn(implIt);
        PowerMockito.mockStatic(ServiceLoader.class);
        when(ServiceLoader.load(service)).thenReturn(fakeService);
        when(ServiceLoader.load(eq(service), any(ClassLoader.class)))
                .thenReturn(fakeService);
    }
}
