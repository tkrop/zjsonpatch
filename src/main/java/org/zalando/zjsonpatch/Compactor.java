package org.zalando.zjsonpatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.zalando.zjsonpatch.OpType.ADD;
import static org.zalando.zjsonpatch.OpType.MOVE;
import static org.zalando.zjsonpatch.OpType.REMOVE;

/**
 * Merging remove and add to a single move operation.
 */
final class Compactor {

    @SuppressWarnings("unused")
    private final Set<FeatureFlags> flags;

    public Compactor(Set<FeatureFlags> flags) {
        this.flags = flags;
    }

    public void compact(final List<Patch> patches) {
        for (int i = 0; i < patches.size(); i++) {
            Patch patch1 = patches.get(i);

            // if not remove OR add, move to next patch
            if (!(REMOVE.equals(patch1.type) || ADD.equals(patch1.type))) {
                continue;
            }

            for (int j = i + 1; j < patches.size(); j++) {
                Patch patch2 = patches.get(j);
                if (!patch1.value.equals(patch2.value)) {
                    continue;
                }

                Patch moveDiff = null;
                if (REMOVE.equals(patch1.type) && ADD.equals(patch2.type)) {
                    computeRelativePath(patch2.path, i + 1, j - 1, patches);
                    moveDiff = new Patch(MOVE, patch1.path, patch2.path, patch2.value);
                } else if (ADD.equals(patch1.type) && REMOVE.equals(patch2.type)) {
                    computeRelativePath(patch2.path, i, j - 1, patches); // patch1's add should also be considered
                    moveDiff = new Patch(MOVE, patch2.path, patch1.path, patch1.value);
                }
                if (moveDiff != null) {
                    patches.remove(j);
                    patches.set(i, moveDiff);
                    break;
                }
            }
        }
    }

    // Note : only to be used for arrays
    // Finds the longest common Ancestor ending at Array
    private void computeRelativePath(List<Object> path, int startIdx, int endIdx, List<Patch> patches) {
        List<Integer> counters = new ArrayList<Integer>();

        resetCounters(counters, path.size());

        for (int i = startIdx; i <= endIdx; i++) {
            Patch patch = patches.get(i);
            // Adjust relative path according to #Add and #Remove
            if (ADD.equals(patch.type) || REMOVE.equals(patch.type)) {
                updatePath(path, patch, counters);
            }
        }
        updatePathWithCounters(counters, path);
    }

    private void resetCounters(List<Integer> counters, int size) {
        for (int i = 0; i < size; i++) {
            counters.add(0);
        }
    }

    private void updatePathWithCounters(List<Integer> counters, List<Object> path) {
        for (int i = 0; i < counters.size(); i++) {
            int value = counters.get(i);
            if (value != 0) {
                Integer currValue = Integer.parseInt(path.get(i).toString());
                path.set(i, String.valueOf(currValue + value));
            }
        }
    }

    private void updatePath(List<Object> path, Patch pseudo, List<Integer> counters) {
        // find longest common prefix of both the paths
        if (pseudo.path.size() <= path.size()) {
            int idx = -1;
            for (int i = 0; i < pseudo.path.size() - 1; i++) {
                if (pseudo.path.get(i).equals(path.get(i))) {
                    idx = i;
                } else {
                    break;
                }
            }
            if ((idx == pseudo.path.size() - 2)
                    && (pseudo.path.get(pseudo.path.size() - 1) instanceof Integer)) {
                updateCounters(pseudo, pseudo.path.size() - 1, counters);
            }
        }
    }

    private void updateCounters(Patch pseudo, int idx, List<Integer> counters) {
        if (ADD.equals(pseudo.type)) {
            counters.set(idx, counters.get(idx) - 1);
        } else if (REMOVE.equals(pseudo.type)) {
            counters.set(idx, counters.get(idx) + 1);
        }
    }
}