package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;

public class TestCase {

    private final ObjectNode node;

    private TestCase(ObjectNode node) {
        this.node = node;
    }

    public boolean isOperation() {
        return !node.has("error");
    }

    public ObjectNode getNode() {
        return node;
    }

    public TestCase addFalgs(Set<CompatibilityFlags> flags) {
        ArrayNode tflags = (ArrayNode) node.get("flags");
        if (tflags == null) {
            tflags = node.putArray("flags");
        }
        for (CompatibilityFlags flag : flags) {
            tflags.add(flag.toString());
        }
        return this;
    }

    public Set<CompatibilityFlags> getFlags() {
        Set<CompatibilityFlags> flags = new HashSet<CompatibilityFlags>();
        if (node.has("flags")) {
            for (JsonNode name : node.get("flags")) {
                if (name != null && name.isTextual()) {
                    flags.add(getFlag(name.asText()));
                }
            }
        }
        return flags.isEmpty() ? null : EnumSet.copyOf(flags);
    }

    private CompatibilityFlags getFlag(String name) {
        for (CompatibilityFlags flag : CompatibilityFlags.values()) {
            if (flag.name().equalsIgnoreCase(name)) {
                return flag;
            }
        }
        throw new IllegalArgumentException("flag not found [" + name + "]");
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Collection<TestCase> load(String file) throws IOException {
        String path = "/testdata/" + file + ".json";
        InputStream stream = TestCase.class.getResourceAsStream(path);
        String date = IOUtils.toString(stream, "UTF-8");
        JsonNode tree = MAPPER.readTree(date);

        List<TestCase> result = new ArrayList<TestCase>();
        if (tree.isArray()) {
            for (JsonNode node : tree) {
                if (isEnabled(node)) {
                    result.add(new TestCase((ObjectNode) node));
                }
            }
        } else if (tree.isObject()) {
            if (tree.has("errors")) {
                for (JsonNode node : tree.get("errors")) {
                    if (isEnabled(node)) {
                        ((ObjectNode) node).putNull("error");
                        result.add(new TestCase((ObjectNode) node));
                    }
                }
            }
            if (tree.has("ops")) {
                for (JsonNode node : tree.get("ops")) {
                    if (isEnabled(node)) {
                        result.add(new TestCase((ObjectNode) node));
                    }
                }
            }
        }
        return result;
    }

    private static boolean isEnabled(JsonNode node) {
        if (!node.isObject()) {
            return false;
        }
        JsonNode disabled = node.get("disabled");
        return (disabled == null || disabled.isBoolean() && !disabled.booleanValue());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (node.has("message")) {
            builder.append(node.get("message").asText());
        }
        if (node.has("flags")) {
            if (builder.length() != 0) {
                builder.append(' ');
            }
            builder.append(getFlags().toString());
        }
        if (builder.length() == 0) {
            builder.append(super.toString());
        }
        return builder.toString();
    }
}
