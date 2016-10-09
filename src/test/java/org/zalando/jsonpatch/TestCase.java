package org.zalando.jsonpatch;

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
import java.util.Scanner;
import java.util.Set;

import org.zalando.jsonpatch.FeatureFlags;

public class TestCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ObjectNode node;

    private TestCase(ObjectNode node) {
        this.node = node;
    }

    public static Collection<TestCase> load(String file) throws IOException {
        String path = "/testdata/" + file + ".json";
        JsonNode tree = MAPPER.readTree(read(path));

        List<TestCase> result = new ArrayList<TestCase>();
        if (tree.isArray()) {
            for (JsonNode node : tree) {
                if (node.isObject()) {
                    result.add(new TestCase((ObjectNode) node));
                }
            }
        } else if (tree.isObject()) {
            if (tree.has("errors")) {
                for (JsonNode node : tree.get("errors")) {
                    if (node.isObject()) {
                        ((ObjectNode) node).putNull("error");
                        result.add(new TestCase((ObjectNode) node));
                    }
                }
            }
            if (tree.has("ops")) {
                for (JsonNode node : tree.get("ops")) {
                    if (node.isObject()) {
                        result.add(new TestCase((ObjectNode) node));
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("resource")
    private static String read(String path) throws IOException {
        InputStream stream = TestCase.class.getResourceAsStream(path);
        try {
            Scanner scanner = new Scanner(stream).useDelimiter("\\A");
            try {
                return scanner.hasNext() ? scanner.next() : "";
            } finally {
                scanner.close();
            }
        } finally {
            stream.close();
        }
    }

    public boolean isEnabled() {
        JsonNode disabled = node.get("disabled");
        if (disabled == null) {
            return true;
        } else if (disabled.isBoolean()) {
            return !disabled.booleanValue();
        } else if (disabled.isArray()) {
            Set<FeatureFlags> flags = getFlags();
            for (FeatureFlags flag : parseFlags(disabled)) {
                if (flags != null && flags.contains(flag)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean isOperation() {
        return !node.has("error");
    }

    public ObjectNode getNode() {
        return node;
    }

    public TestCase addFalgs(Set<FeatureFlags> flags) {
        if (flags == null) {
            return this;
        }
        ArrayNode tflags = (ArrayNode) node.get("flags");
        if (tflags == null) {
            tflags = node.putArray("flags");
        }
        for (FeatureFlags flag : flags) {
            tflags.add(flag.toString());
        }
        return this;
    }

    public Set<FeatureFlags> getFlags() {
        if (node.has("flags")) {
            return parseFlags(node.get("flags"));
        }
        return null;
    }

    private Set<FeatureFlags> parseFlags(JsonNode node) {
        Set<FeatureFlags> flags = new HashSet<FeatureFlags>();
        for (JsonNode name : node) {
            if (name != null && name.isTextual()) {
                flags.add(parseFlag(name.asText()));
            }
        }
        return (!flags.isEmpty()) ? EnumSet.copyOf(flags) : null;
    }

    private FeatureFlags parseFlag(String name) {
        for (FeatureFlags flag : FeatureFlags.values()) {
            if (flag.name().equalsIgnoreCase(name)) {
                return flag;
            }
        }
        throw new IllegalArgumentException("flag not found [" + name + "]");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (node.has("message")) {
            builder.append(node.get("message").asText());
        }
        if (node.has("flags")) {
            Set<FeatureFlags> flags = getFlags();
            if (flags != null) {
                if (builder.length() != 0) {
                    builder.append(' ');
                }
                builder.append(flags.toString());
            }
        }
        if (builder.length() == 0) {
            builder.append(super.toString());
        }
        return builder.toString();
    }
}
