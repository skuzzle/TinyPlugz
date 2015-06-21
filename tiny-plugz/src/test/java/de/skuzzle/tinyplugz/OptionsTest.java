package de.skuzzle.tinyplugz;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

public class OptionsTest {

    @Test
    public void testSetContainsAll() throws Exception {
        final Set<String> expected = Arrays.stream(Options.class.getFields())
                .filter(field -> Modifier.isPublic(field.getModifiers()))
                .filter(field -> Modifier.isFinal(field.getModifiers()))
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getType() == String.class)
                .map(field -> { try {
                    return field.get(null);
                } catch (final Exception e) {
                    return null;
                }})
                .filter(obj -> obj != null)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertEquals(expected, Options.ALL_DEFAULT_OPTIONS);
    }
}
