package org.zalando.jsonpatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.Assert;
import org.junit.Test;

public class CoverageTest {
    @Test
    public void create() {
        Assert.assertThat(new JsonPatch() {
            // nothing to do.
        }, is(notNullValue()));
        Assert.assertThat(new PathHelper() {
            // nothing to do.
        }, is(notNullValue()));
        Assert.assertThat(new Constants() {
            // nothing to do.
        }, is(notNullValue()));
    }
}
