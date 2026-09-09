package com.kael.guard.checks;

import com.kael.guard.KaelorvynGuard;
import com.kael.guard.memory.MemoryStore;

import java.util.HashMap;
import java.util.Map;

public final class AntibodyLibrary {

    private final KaelorvynGuard plugin;
    private final MemoryStore store;
    private final Map<String, double[]> antibodies = new HashMap<>();

    public AntibodyLibrary(KaelorvynGuard plugin, MemoryStore store) {
        this.plugin = plugin;
        this.store = store;
        antibodies.putAll(store.antibodies());
    }

    public void learn(String family, double[] vector) {
        if (!antibodies.containsKey(family) && antibodies.size() >= plugin.settings().antibodyMax()) {
            plugin.getLogger().warning("抗体库已满（" + plugin.settings().antibodyMax()
                    + "），忽略新家族：" + family);
            return;
        }
        antibodies.put(family, vector.clone());
        store.saveAntibody(family, vector);
        plugin.getLogger().info("抗体库已更新：" + family);
    }

    public void learnOrMerge(String family, double[] vector) {
        double[] existing = antibodies.get(family);
        if (existing == null) {
            learn(family, vector);
            return;
        }
        int n = Math.min(existing.length, vector.length);
        for (int i = 0; i < n; i++) existing[i] = (existing[i] + vector[i]) / 2.0;
        store.saveAntibody(family, existing);
        plugin.getLogger().info("抗体库已合并更新：" + family);
    }

    public CheckResult match(double[] vector) {
        if (antibodies.isEmpty()) return null;
        String bestFamily = null;
        double best = 0;
        for (Map.Entry<String, double[]> e : antibodies.entrySet()) {
            if (e.getKey().equalsIgnoreCase("ANTIBODY")) continue;
            double sim = cosine(vector, e.getValue());
            if (sim > best) {
                best = sim;
                bestFamily = e.getKey();
            }
        }
        if (bestFamily != null && best >= plugin.settings().antibodyMatchThreshold()) {
            return CheckResult.signature("ANTIBODY", bestFamily, best,
                    "匹配已知外挂家族 " + bestFamily);
        }
        return null;
    }

    public int size() {
        return antibodies.size();
    }

    private double cosine(double[] a, double[] b) {
        double dot = 0, na = 0, nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
