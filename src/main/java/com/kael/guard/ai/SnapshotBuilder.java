package com.kael.guard.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kael.guard.data.PlayerData;
import com.kael.guard.memory.MemoryStore;
import com.kael.guard.stats.StatsUtil;

import java.util.Map;

public final class SnapshotBuilder {

    private static final Gson GSON = new Gson();

    private SnapshotBuilder() {
    }

    public static String build(PlayerData d, MemoryStore store) {
        JsonObject root = new JsonObject();
        JsonObject player = new JsonObject();
        player.addProperty("name", d.name);
        player.addProperty("ping", d.ping);
        player.addProperty("playtimeMinutes", 0);
        player.addProperty("warnings", store.warnings(d.uuid));
        player.addProperty("brand", d.brand);
        player.addProperty("usingItem", d.usingItem);
        player.addProperty("water", d.water);
        player.addProperty("swimming", d.swimming);
        player.addProperty("onGround", d.claimedOnGround);
        player.addProperty("lastWaterMsAgo", d.lastWaterTime == 0 ? -1 : System.currentTimeMillis() - d.lastWaterTime);
        player.add("verdictsByCategory", GSON.toJsonTree(store.verdictCounts(d.uuid)));
        root.add("player", player);

        JsonObject window = new JsonObject();
        long cps = d.attackIntervals.stream().filter(t -> t >= System.currentTimeMillis() - 1000).count();
        double clickEntropy = StatsUtil.entropy(d.attackIntervals, 5.0);
        double aimGcd = StatsUtil.gcd(d.yawDeltas);
        double aimMean = StatsUtil.mean(d.yawDeltas);
        double aimStd = StatsUtil.stdDev(d.yawDeltas, aimMean);
        double moveMean = StatsUtil.mean(d.moveIntervalsMs);
        double moveStd = StatsUtil.stdDev(d.moveIntervalsMs, moveMean);
        double reachMean = StatsUtil.mean(d.reachSamples);
        window.addProperty("cps", cps);
        window.addProperty("clickEntropy", clickEntropy);
        window.addProperty("aimGcd", aimGcd);
        window.addProperty("aimStd", aimStd);
        window.addProperty("moveIntervalMeanMs", moveMean);
        window.addProperty("moveIntervalStdMs", moveStd);
        window.addProperty("reachMean", reachMean);
        window.addProperty("airTicks", d.airTicks);
        window.addProperty("oreRatio", d.blocksBroken > 0 ? (double) d.oresBroken / d.blocksBroken : 0);
        window.add("counts", GSON.toJsonTree(Map.of(
                "attack", d.eventCount("attack"),
                "swing", d.eventCount("swing"),
                "place", d.eventCount("place"),
                "break", d.eventCount("break"),
                "timer", d.eventCount("timer"),
                "reach", d.eventCount("reach"),
                "nofall", d.eventCount("nofall"),
                "packetfly", d.eventCount("packetfly"),
                "xray", d.eventCount("xray"),
                "autoswitch", d.eventCount("autoswitch"))));
        root.add("window", window);

        JsonArray events = new JsonArray();
        for (String e : d.recentEvents) events.add(e);
        root.add("recentEvents", events);
        return GSON.toJson(root);
    }
}
