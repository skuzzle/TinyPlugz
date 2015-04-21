package de.skuzzle.tinyplugz;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RequireTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testRequireNull() throws Exception {
        Require.nonNull(new Object(), "foo");
    }

    @Test
    public void testNonNullWithNullValue() throws Exception {
        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage("'foo' must not be null");
        Require.nonNull(null, "foo");
    }

    @Test
    public void testNonNullWithNullValueAndNullName() throws Exception {
        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage("'null' must not be null");
        Require.nonNull(null, null);
    }

    @Test
    public void testConditionWithFalseCondition() throws Exception {
        this.exception.expect(IllegalArgumentException.class);
        this.exception.expectMessage("foo bar");
        Require.condition(false, "foo %s", "bar");
    }

    @Test
    public void testConditionWithTrueCondition() throws Exception {
        Require.condition(true, "foo", "bar");
    }

    @Test
    public void testStateWithFalseCondition() throws Exception {
        this.exception.expect(IllegalStateException.class);
        this.exception.expectMessage("foo bar");
        Require.state(false, "foo %s", "bar");
    }

    @Test
    public void testStateWithTrueCondition() throws Exception {
        Require.state(true, "foo", "bar");
    }
}
