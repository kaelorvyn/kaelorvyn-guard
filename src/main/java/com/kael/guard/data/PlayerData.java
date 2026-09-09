package com.kael.guard.data;

import com.kael.guard.stats.StatsUtil;
import com.kael.guard.stats.AdaptiveScore;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {

    public final UUID uuid;
    public final String name;

    public double lastX, lastY, lastZ;
    public float lastYaw, lastPitch;
    public long lastMoveTime;
    public boolean claimedOnGround;
    public int groundTicks, airTicks;
    public int ping;
    public double walkSpeed = 0.2;
    public int speedAmp = -1, slownessAmp = -1, jumpAmp = -1;
    public boolean flying, gliding, vehicle, water, creative, sprinting, sneaking, climbing;
    public long lastTeleport;
    public long joinTime;
    public long lastWaterTime;
    public int slowFallTicks;
    public long lastSlowFallEmit;

    public final Deque<Long> moveIntervalsMs = new ArrayDeque<>();
    public final Deque<Float> yawDeltas = new ArrayDeque<>();
    public final Deque<Long> attackIntervals = new ArrayDeque<>();
    public final Deque<Long> attackTimes = new ArrayDeque<>();
    public final Deque<Double> reachSamples = new ArrayDeque<>();
    public final Deque<Long> placeTimes = new ArrayDeque<>();
    public final Deque<Long> scaffoldPlaces = new ArrayDeque<>();
    public final Deque<Long> breakTimes = new ArrayDeque<>();
    public final Deque<String> recentEvents = new ArrayDeque<>();

    public long lastAttackTime;
    public long lastSwingTime;
    public long lastBlinkEmit;
    public int attacksThisTick, swingsThisTick, breaksThisTick, clicksThisTick;
    public int lastHeldSlot = -1;
    public long lastHeldSlotTime;
    public int burstPackets;
    public long timerBalance;
    public long lastTransaction;
    public long lastCandidateSubmit;
    public String lastCandidateReason = "";
    public double lastPacketY;
    public int meteorAntiKickHits;
    public long meteorAntiKickFirstHit;
    public int packetFlyDualHits;
    public long packetFlyDualFirstHit;
    public float lastYawDelta;
    public double lastHorizontal;

    public String brand = "";
    public long lastRespawn;
    public long lastVelocityTime;
    public long expectedKnockbackUntil;
    public int flyingPacketsSinceTickEnd;
    public long lastTickEnd;
    public int oresBroken, blocksBroken;
    public int noSwingAttacks;
    public boolean usingItem;
    public final java.util.Map<Integer, Long> targetTimes = new java.util.HashMap<>();
    public final java.util.ArrayDeque<double[]> recentMoveDeltas = new java.util.ArrayDeque<>();
    public final java.util.ArrayDeque<Integer> pingSamples = new java.util.ArrayDeque<>();
    public int trustLevel = 3;
    public int auraBotId = -1;
    public long auraBotSpawnedAt;
    public String lastWindowClickType;
    public long lastWindowClickTime;
    public int placesThisTick, lookPacketsThisTick;
    public long lastPlaceTime;
    public int lastPlaceX, lastPlaceY, lastPlaceZ;
    public int queuedAttackEntityId = -1;
    public long queuedAttackTime;
    public long lastInteractAttackTime;
    public int protocolVersion;
    public int ascentTicks;
    public double ascentTotal;
    public boolean slowFalling;
    public boolean swimming;
    public long lastFlightEmit;
    public int airJumpPulses;
    public long lastAirJumpPulse;
    public int jesusPulses;
    public long jesusFirstPulseTime;
    public int spiderTicks;
    public double lastDy;
    public long lastUseTime;
    public long lastDigStartTime;
    public int kbWeakTicks;
    public double expectedKbSpeed;
    public final java.util.ArrayDeque<Long> useTimes = new java.util.ArrayDeque<>();
    public final java.util.ArrayDeque<Long> useTimesStamps = new java.util.ArrayDeque<>();
    public final java.util.ArrayDeque<Long> windowClicks = new java.util.ArrayDeque<>();
    public final java.util.ArrayDeque<Long> actualDamageTimes = new java.util.ArrayDeque<>();
    public final java.util.ArrayDeque<Long> externalImpulseTimes = new java.util.ArrayDeque<>();

    public long breakStartTime = -1;
    public double breakStartX, breakStartY, breakStartZ;
    public long lastMoveIntervalAvg;

    private final Map<String, AdaptiveScore> scores = new HashMap<>();
    private final Map<String, Integer> eventCount = new HashMap<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public void addEvent(String text) {
        recentEvents.addLast(text);
        while (recentEvents.size() > 20) recentEvents.removeFirst();
    }

    public double observeCheck(String category, double evidence) {
        AdaptiveScore score = scores.computeIfAbsent(category, k -> new AdaptiveScore(0.25));
        return score.observe(evidence);
    }

    public AdaptiveScore score(String category) {
        return scores.computeIfAbsent(category, k -> new AdaptiveScore(0.25));
    }

    public void countEvent(String key) {
        eventCount.merge(key, 1, Integer::sum);
    }

    public int eventCount(String key) {
        return eventCount.getOrDefault(key, 0);
    }

    public double[] featureVector() {
        double cps = attackIntervals.stream().filter(t -> t >= System.currentTimeMillis() - 1000).count();
        double clickEntropy = StatsUtil.entropy(attackIntervals, 5.0);
        double aimGcd = StatsUtil.gcd(yawDeltas);
        double moveMean = StatsUtil.mean(moveIntervalsMs);
        double moveStd = StatsUtil.stdDev(moveIntervalsMs, moveMean);
        double[] v = new double[16];
        v[0] = normalize(eventCount("attack"), 200);
        v[1] = normalize(eventCount("swing"), 200);
        v[2] = normalize(eventCount("place"), 100);
        v[3] = normalize(eventCount("break"), 100);
        v[4] = normalize((int) cps, 30);
        v[5] = clickEntropy > 0 ? Math.min(1, clickEntropy / 5.0) : 0;
        v[6] = aimGcd > 0 ? Math.min(1, aimGcd * 1000) : 0;
        v[7] = normalize((int) moveMean, 100);
        v[8] = normalize((int) moveStd, 100);
        v[9] = normalize(airTicks, 100);
        v[10] = normalize((int) lastMoveIntervalAvg, 200);
        v[11] = normalize(eventCount("timer"), 50);
        v[12] = normalize(eventCount("reach"), 20);
        v[13] = normalize(eventCount("nofall"), 20);
        v[14] = normalize(eventCount("packetfly"), 20);
        v[15] = normalize(eventCount("step") + eventCount("slowfall") + eventCount("flight"), 20);
        return v;
    }

    private static double normalize(int value, int cap) {
        return Math.min(1.0, Math.max(0.0, (double) value / Math.max(1, cap)));
    }
}
