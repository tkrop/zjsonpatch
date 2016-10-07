package org.zalando.jsonpatch;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.zalando.jsonpatch.FeatureFlags;
import org.zalando.jsonpatch.JsonPatch;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Set;

@RunWith(Parameterized.class)
public abstract class AbstractPatchTest {

    @Parameter
    public TestCase p;

    private JsonNode operation(JsonNode source, JsonNode target, JsonNode patch) {
        Set<FeatureFlags> flags = p.getFlags();
        if (flags == null) {
            if (patch == null && target != null) {
                patch = JsonDiff.asJson(source, target);
            }
            JsonPatch.validate(patch);
            return JsonPatch.apply(patch, source);
        }
        if (patch == null && target != null) {
            patch = JsonDiff.asJson(source, target, flags);
        }
        JsonPatch.validate(patch, flags);
        return JsonPatch.apply(patch, source, flags);
    }

    @Test
    public void test() throws Exception {
        if (p.isOperation()) {
            testOpertaion();
        } else {
            testError();
        }
    }

    private void testOpertaion() throws Exception {
        JsonNode node = p.getNode();

        JsonNode source = node.get("source");
        JsonNode target = node.get("target");
        JsonNode patch = node.get("patch");
        String message = node.has("message") ? node.get("message").toString() : "";

        final JsonNode actualSecond = operation(source, target, patch);

        assertThat(message, actualSecond, equalTo(target));
    }

    private void testError() {
        JsonNode node = p.getNode();

        JsonNode source = node.get("source");
        JsonNode target = node.get("target");
        JsonNode patch = node.get("patch");
        String error = node.has("error") && !node.get("error").isNull() ? node.get("error").asText() : null;

        try {
            operation(source, target, patch);
            fail("Failure expected: " + ((error != null) ? error : node.get("message")));
        } catch (Exception ex) {
            if (error != null) {
                assertThat(ex.toString(), containsString(error));
            }
        }
    }
}
