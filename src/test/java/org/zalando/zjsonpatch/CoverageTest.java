package org.zalando.zjsonpatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.Assert;
import org.junit.Test;
import org.zalando.zjsonpatch.Constants;
import org.zalando.zjsonpatch.JsonPatch;
import org.zalando.zjsonpatch.PathHelper;

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
