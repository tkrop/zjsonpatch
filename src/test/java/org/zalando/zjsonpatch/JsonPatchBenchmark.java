package org.zalando.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.zalando.zjsonpatch.FeatureFlags;
import org.zalando.zjsonpatch.JsonPatch;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.zalando.zjsonpatch.FeatureFlags.LCS_ITERATE_PATCH_GENERATOR;
import static org.zalando.zjsonpatch.FeatureFlags.LCS_VISIT_PATCH_GENERATOR;
import static org.zalando.zjsonpatch.FeatureFlags.PATCH_OPTIMIZATION;
import static org.zalando.zjsonpatch.FeatureFlags.SIMPLE_COMPARE_PATCH_GENERATOR;

public class JsonPatchBenchmark {
    private static final int SOURCE_ITEMS = 2;
    private static final int TARGET_ITEMS = 2;
    private static final int SOURCE_PRICES = 0;
    private static final int TARGET_PRICES = 3;
    private static final int SOURCE_CUSTOMS = 0;
    private static final int TARGET_CUSTOMS = 2;
    private static final int[] SOURCE_PROPS = new int[] { 13, 2, 8, 3, 0 };// new int[] { 4, 2, 2, 0 };
    private static final int[] TARGET_PROPS = new int[] { 13, 3, 8, 3, 2 };
    private static final int MAX_CHANGES = 800; // Integer.MAX_VALUE;

    private static final int SIZE = 20000;
    private static final int ITER = Integer.MAX_VALUE;
    private static final int WARMS = 2;
    private static final int RUNS = 20;
    private static final int LOOPS = 40;
    private static final Set<FeatureFlags> FLAGS = //
            EnumSet.of(LCS_ITERATE_PATCH_GENERATOR);

    @SuppressWarnings("unchecked")
    private static final Set<FeatureFlags>[] FLAGS_SET = new Set[] {
            EnumSet.of(LCS_VISIT_PATCH_GENERATOR, PATCH_OPTIMIZATION),
            EnumSet.of(LCS_VISIT_PATCH_GENERATOR),
            EnumSet.of(LCS_ITERATE_PATCH_GENERATOR),
            EnumSet.of(SIMPLE_COMPARE_PATCH_GENERATOR)
    };

    private static final String[] TOUR_PROPS =
            { "id", "created_at", "modified_at", "completed", "closed_at", "transport_number", "tour_number",
                    "warehouse_id", "logistics_provider_name", "destination_country", "export_country",
                    "import_country", "exchange_rate" };
    private static final String[] SHIPMENT_PROPS = { "id", "tracking_number" };
    private static final String[] ITEM_PROPS = { "id", "product_id", "order_number", "ordered_at", "ean",
            "tariff_number", "description", "country_of_origin", "preferential_origin",
            "volatile_organic_compounds_percentage", "set_item_quantity", "gross_purchase_price",
            "gross_retail_price", "gross_discounted_price", "tax", "net_weight", "gross_weight", "package_weight" };
    private static final String[] PRICE_PROPS = { "value", "curreny", "flags" };
    private static final String[] CUSTOMS_PROPS =
            { "tariff_number", "description", "gross_retail_price", "gross_discounted_price", "tax" };

    public static void main(String[] args) throws InterruptedException {
        final Random random = new Random();
        final JsonNodeFactory factory = new JsonNodeFactory(true);
        final ObjectNode[] source = new ObjectNode[RUNS];
        final ObjectNode[] target = new ObjectNode[RUNS];
        @SuppressWarnings("unchecked")
        final List<JsonNode>[] patch = new List[RUNS];

        checkJsonDiff(random, factory, source, target, patch);
    }

    private static void checkMemory(int sleep) throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long free = 0, start = System.nanoTime();
        System.out.print("Clean memory [used=" + used + "] => ");
        while (free != runtime.freeMemory()) {
            System.out.print(".");
            free = runtime.freeMemory();
            System.gc();
            System.runFinalization();
            Thread.sleep(sleep);
        }
        double time = (System.nanoTime() - start) / 1e9;
        used = runtime.totalMemory() - runtime.freeMemory();
        System.out.print(" [used=" + used + ", time=" + time + "s]\n");
    }

    public static void checkJsonDiff(final Random random, final JsonNodeFactory factory, final ObjectNode[] source,
            final ObjectNode[] target, final List<JsonNode>[] patch) throws InterruptedException {
        for (int size = 2048; size <= 32768; size *= 2) {
            create(random, factory, source, target, FLAGS, size);
            checkMemory(200);
            for (Set<FeatureFlags> flags : FLAGS_SET) {
                create(source, target, patch, flags, size, LOOPS);
                // evaluate("actual", factory, source, target, patch, flags, RUNS);
                checkMemory(200);
            }
        }
    }

    public static void checkJsonPatch(final Random random, final JsonNodeFactory factory, final ObjectNode[] source,
            final ObjectNode[] target, final List<JsonNode>[] patch) {
        create(random, factory, source, target, patch, FLAGS, SIZE);
        evaluate("warmup", factory, source, target, patch, FLAGS, WARMS);
        evaluate("actual", factory, source, target, patch, FLAGS, RUNS);
    }

    private static List<JsonNode> createJsonPatchList(JsonNode source, JsonNode target, Set<FeatureFlags> flags) {
        JsonNode patch = JsonPatch.create(source, target, flags);
        List<JsonNode> list = new ArrayList<JsonNode>(patch.size());
        Iterator<JsonNode> iter = patch.iterator();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }

    private static void create(final Random random, final JsonNodeFactory factory, final ObjectNode[] source,
            final ObjectNode[] target, final List<JsonNode>[] patch, Set<FeatureFlags> flags, int size) {
        create(random, factory, source, target, flags, size);
        create(source, target, patch, flags, size, 1);
    }

    private static void create(final Random random, final JsonNodeFactory factory, final ObjectNode[] source,
            final ObjectNode[] target, Set<FeatureFlags> flags, int size) {
        System.out.print("Setup tours [size=" + size + ", flags=" + flags + "] => ");
        long count = 0, start = System.nanoTime();
        for (int run = 0; run < RUNS; run++) {
            source[run] = createRandomTour(random, new ObjectNode(factory),
                    new int[] { size, SOURCE_ITEMS, SOURCE_PRICES, SOURCE_CUSTOMS }, SOURCE_PROPS);
            System.out.print(".");
            target[run] = extendRandomTour(random, source[run].deepCopy(),
                    new int[] { size, TARGET_ITEMS, TARGET_PRICES, TARGET_CUSTOMS }, TARGET_PROPS, MAX_CHANGES);
            System.out.print(".");
        }
        double time = (System.nanoTime() - start) / 1e9;
        System.out.println("\nSetup tours [size=" + size + ", runs=" + RUNS + ", time=" + time + "s] => " + count);
    }

    private static void create(final ObjectNode[] source, final ObjectNode[] target, final List<JsonNode>[] patch,
            Set<FeatureFlags> flags, int size, int loops) {
        System.out.print("Setup patchs [size=" + size + ", flags=" + flags + "] => ");
        long count = 0, start = System.nanoTime();
        for (int loop = 0; loop < loops; loop++) {
            for (int run = 0; run < RUNS; run++) {
                patch[run] = createJsonPatchList(source[run], target[run], flags);
                // System.out.print("->" + patch[run].size() + " ");
                count += patch[run].size();
            }
        }
        double time = (System.nanoTime() - start) / 1e9;
        System.out.println("\nSetup patchs [size=" + size + ", runs=" + RUNS + ", time=" + time + "s] => " + count);
    }

    private static void evaluate(String name, JsonNodeFactory factory, ObjectNode[] source, ObjectNode[] target,
            final List<JsonNode>[] patch, Set<FeatureFlags> flags, int runs) {
        final JsonNode[] update = new JsonNode[RUNS];
        System.out.print("Running " + name + " [flags=" + flags + "] => ");
        long count = 0, start = System.nanoTime();
        for (int run = 0; run < runs; run++) {
            update[run] = source[run].deepCopy();
            for (int max = patch[run].size(), from = 0, to = Math.min(max, ITER); from < to; from =
                    Math.min(max, to), to = Math.min(max, to + ITER)) {
                ArrayNode subpatch = new ArrayNode(factory).addAll(patch[run].subList(from, to));
                update[run] = JsonPatch.apply(subpatch, update[run], flags);
                System.out.print(".");
            }
            // System.out.print("->" + patch[run].size() + " ");
            count += patch[run].size();
        }
        double time = (System.nanoTime() - start) / 1e9;
        System.out
                .println("\nRun " + name + " [flags=" + flags + ", runs=" + runs + ", time=" + time + "s] => " + count);

        count = 0;
        start = System.nanoTime();
        // System.out.print("Validate " + name + " => ");
        for (int run = 0; run < runs; run++) {
            List<JsonNode> diffs = createJsonPatchList(update[run], target[run], flags);
            if (diffs.size() > 0) {
                throw new RuntimeException("not equals" +
                        "\n  source = " + source[run] +
                        "\n  update = " + update[run] +
                        "\n  target = " + target[run] +
                        "\n  patch = " + patch[run] +
                        "\n  diffs = " + diffs);
            }
            // System.out.print(".");
            count += diffs.size();
        }
        time = (System.nanoTime() - start) / 1e9;
        // System.out.println("\nValidate " + name + " [runs=" + runs + ", time=" + time + "s] => " + count);
    }

    private static ObjectNode createRandomTour(final Random random, final ObjectNode tour, int[] nums, int[] props) {
        createRandomProps(tour, random, TOUR_PROPS, 12, props[0]);
        final ArrayNode shipments = tour.putArray("shipments");
        for (int scount = 0; scount < nums[0]; scount++) {
            final ObjectNode shipment = createRandomProps(shipments.addObject(), random, SHIPMENT_PROPS, 2, props[1]);
            final ArrayNode items = shipment.putArray("items");
            for (int icount = 0; icount < ((nums[1] > 0) ? random.nextInt(nums[1]) + 1 : 0); icount++) {
                final ObjectNode item = createRandomProps(items.addObject(), random, ITEM_PROPS, 3, props[2]);
                if (item.size() < 3) {
                    throw new RuntimeException(item.toString());
                }
                final ArrayNode prices = item.putArray("prices");
                int num = (nums[2] > 0) ? random.nextInt(nums[2]) : 0;
                for (int pcount = 0; pcount < num; pcount++) {
                    createRandomProps(prices.addObject(), random, PRICE_PROPS, 0, props[3]);
                }
                final ArrayNode customs = item.putArray("customs");
                int cnum = (nums[2] > 0) ? random.nextInt(nums[2]) : 0;
                for (int ccount = 0; ccount < cnum; ccount++) {
                    createRandomProps(customs.addObject(), random, CUSTOMS_PROPS, 0, props[3]);
                }
            }
        }
        return tour;
    }

    private static ObjectNode createRandomProps(final ObjectNode node, final Random random, final String[] name,
            int min, int max) {
        int num = (max - min > 0) ? min + random.nextInt(max - min) : max;
        for (int prop = 0; prop < num; prop++) {
            node.put(name[prop], UUID.randomUUID().toString());
        }
        return node;
    }

    private static ObjectNode extendRandomTour(final Random random, final ObjectNode node,
            int[] nums, int[] props, int max) {
        int changes = extendRandomProps(node, random, TOUR_PROPS, 12, props[0]);
        final ArrayNode shipments = putArrayIfAbsent(node, "shipments");
        for (int scount = 0; scount < nums[0] && changes < max; scount++) {
            final ObjectNode shipment =
                    (scount < shipments.size()) ? (ObjectNode) shipments.get(scount) : shipments.addObject();
            changes += extendRandomProps(shipment, random, SHIPMENT_PROPS, 2, props[1]);
            final ArrayNode items = putArrayIfAbsent(shipment, "items");
            int snum = (nums[1] > 0) ? random.nextInt(nums[1]) + 1 : 0;
            for (int icount = 0; icount < snum && changes < max; icount++) {
                final ObjectNode item = (icount < items.size()) ? (ObjectNode) items.get(icount) : items.addObject();
                changes += extendRandomProps(item, random, ITEM_PROPS, 3, props[2]);
                if (item.size() < 3) {
                    throw new RuntimeException(item.toString());
                }
                final ArrayNode prices = putArrayIfAbsent(item, "prices");
                int pnum = (nums[2] > 0) ? random.nextInt(nums[2]) : 0;
                for (int pcount = 0; pcount < pnum && changes < max; pcount++) {
                    ObjectNode price = (pcount < prices.size()) ? (ObjectNode) prices.get(pcount) : prices.addObject();
                    changes += extendRandomProps(price, random, PRICE_PROPS, 0, props[3]);
                }
                final ArrayNode customs = putArrayIfAbsent(item, "prices");
                int cnum = (nums[3] > 0) ? random.nextInt(nums[3]) : 0;
                for (int ccount = 0; ccount < cnum && changes < max; ccount++) {
                    ObjectNode custom =
                            (ccount < customs.size()) ? (ObjectNode) customs.get(ccount) : customs.addObject();
                    changes += extendRandomProps(custom, random, CUSTOMS_PROPS, 0, props[3]);
                }
            }
        }
        return node;
    }

    private static int extendRandomProps(final ObjectNode node, final Random random, final String[] name,
            int min, int max) {
        int size = node.size(), num = (max - min > 0) ? min + random.nextInt(max - min) : max;
        for (int prop = size; prop < num; prop++) {
            String key = name[prop];
            if (!node.has(key)) {
                node.put(key, UUID.randomUUID().toString());
            }
        }
        return node.size() - size;
    }

    private static ArrayNode putArrayIfAbsent(final ObjectNode node, String fieldName) {
        return (node.has(fieldName) ? (ArrayNode) node.get(fieldName) : node.putArray(fieldName));
    }
}
