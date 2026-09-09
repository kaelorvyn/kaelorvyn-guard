package com.kael.guard;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

public class Settings {

    private final KaelorvynGuard plugin;

    private boolean packetEnabled;
    private boolean activeDetectionEnabled;
    private boolean flightEnabled, speedEnabled, noFallEnabled, phaseEnabled, timerEnabled;
    private boolean reachEnabled, aimEnabled, clickerEnabled, auraEnabled;
    private boolean scaffoldEnabled, fastBreakEnabled, multiBreakEnabled;
    private boolean elytraEnabled, vehicleEnabled, climbEnabled, fastFallEnabled, blinkEnabled;
    private boolean multiTargetEnabled, noSwingEnabled, knockbackEnabled, fastPlaceEnabled;
    private boolean tickTimerEnabled, aimSnapEnabled, xrayEnabled;
    private boolean airJumpEnabled, spiderEnabled, jesusEnabled, noSlowdownEnabled;
    private boolean criticalsEnabled, throughWallsEnabled, airPlaceEnabled, instantMineEnabled, packetSpamEnabled;
    private boolean attackWhileMiningEnabled, airLiquidBreakEnabled, animationSpamEnabled, crashEnabled;
    private boolean chatEnabled, chatNewlineEnabled;
    private boolean attackRecheckEnabled, wrongToolEnabled, multiPlaceEnabled, lookPacketsEnabled;
    private boolean slowFallEnabled, stepEnabled;
    private boolean trustEnabled, mitigationEnabled, backtrackEnabled;
    private int minAirTicks;
    private double maxUpSpeed, speedTolerance, maxFallDistance;
    private long timerDriftMs;
    private int burstPackets;
    private double baseReach, pingFactor, pingCap, reachMargin;
    private int aimMinSamples, clickerMinSamples;
    private double clickerMaxCps, auraMaxAngle;
    private int scaffoldMaxPlaces;
    private double fastBreakRatio;
    private int multiBreakMax;
    private double elytraMaxSpeed, vehicleMaxSpeed, climbMaxSpeed, fastFallMax;
    private long blinkMinSilenceMs;
    private int multiTargetMax, noSwingAttackThreshold;
    private long noSwingBreakMs;
    private long knockbackWindowMs;
    private int fastPlaceMax, tickEndPackets, snapYaw, snapPrior, minOreSamples;
    private double oreRatio;
    private int flightAscentMinTicks;
    private double flightAscentMinDy, flightAirMoveMinSpeed, flightAirMoveMinDy, flightUpRatio, flightHoverMaxDy;
    private int flightAirMoveExtra, flightHoverExtra;
    private long flightEmitCooldownMs;
    private long speedIntervalMaxMs;
    private double speedAirMultiplier, speedMinHorizontal;
    private int slowFallMinAirTicks, slowFallMinTicks;
    private double slowFallMaxDy, slowFallMinDy;
    private long slowFallWaterExitGraceMs, slowFallEmitCooldownMs;
    private int stepMaxIntervalMs;
    private double stepMinDy, stepMinSum;
    private boolean stepRequireGround;
    private double spiderMaxHorizontal, spiderMinDy, spiderMaxDy;
    private double jesusMaxDy;
    private int jesusPulses;
    private long jesusPulseWindowMs;
    private double jesusJumpRatio;
    private int timerBalanceThreshold;
    private long timerMinIntervalMs;
    private int badPacketsMaxAttacks, badPacketsMaxSwings, badPacketsMaxClicks;
    private double signaturePacketFlyMinDy, signatureDualTolerance, signatureMeteorTolerance;
    private double antibodyLearnMin;
    private boolean auraBotEnabled;
    private long auraBotCooldownMs;
    private double auraBotDistance, auraBotYOffset;
    private int auraBotLifetimeTicks;
    private long blinkAttackRecentMs;
    private long blinkEmitCooldownMs;
    private double knockbackMinRatio;
    private double knockbackMinKbSpeed;
    private double knockbackMinMove;
    private int knockbackWeakTicks;
    private double clickerEntropyMax;
    private long clickerMinIntervalMs;
    private int clickerBinMs;
    private double aimGcdMax, aimStdMax;
    private int airJumpMinAirTicks;
    private double airJumpRatio;
    private int criticalsWindowMs;
    private double criticalsMaxHorizontal;
    private boolean criticalDamageEnabled;
    private boolean phaseOnlyFullBlocks;
    private long exemptionTeleportMs, exemptionRespawnMs, exemptionVelocityMs;
    private long joinGraceMs;
    private double teleportJumpBlocks;
    private int airJumpPulses, spiderTicks, jesusAirTicks, criticalsMoves, instantMineMs, packetSpamMax;
    private double noSlowdownRatio;
    private double clickerCvMax, elytraBoostMax;
    private boolean strictNoItem;
    private double legacyReachMargin;
    private int clickerPerfectRepeat, attackWhileMiningMs, animationSpamFactor, crashWindowClicks, chatSpamPerSecond;
    private int attackRecheckDelayMs;
    private double flightAscentResetDy;
    private int fastFallMaxIntervalMs, fastFallMinAirTicks;
    private long knockbackTimeoutMs;
    private long airJumpPulseWindowMs;
    private double reachThroughWallsMargin;
    private long clickerWindowMs;
    private int animationSpamMinSwings, animationSpamCheckEvery, animationSpamExtraAllowance;
    private long placeDigWindowMs, placeWindowMs;
    private double scaffoldNearDistance;
    private long fastBreakMinExpectedMs, instantMineMinExpectedMs;
    private double wrongToolHandFactor;
    private long windowClickWindowMs, windowPacketOrderMs;
    private int backtrackMinSamples;
    private double backtrackMinStd;
    private int antibodyScanEveryMoves, backtrackScanEveryMoves;
    private int pingSamples, hitboxMaxSnapshots;
    private long hitboxHistoryMs, targetWindowMs;
    private long chatWindowMs;
    private double criticalsTinyMaxDy;
    private long noSwingAttackWindowMs;
    private long autoSwitchWindowMs;
    private int autoSwitchEmitEvery;
    private int trustDefault, trustMin;
    private double mitigationMultiplier, mitigationActivation, backtrackZScore;
    private List<String> mitigationCategories;
    private double wrongToolRatio;
    private int multiPlaceMax, lookPacketsMax;
    private boolean signaturesEnabled;
    private boolean signaturePacketFlyEnabled, signatureDualMoveEnabled, signatureMeteorEnabled;
    private double signaturePacketFlyConfidence, signatureDualMoveConfidence, signatureMeteorConfidence;
    private double signatureDualMoveDy, signatureMeteorDy;
    private int signatureMeteorMinHits;
    private long signatureMeteorWindowMs;
    private int signatureDualMinHits;
    private long signatureDualWindowMs;
    private long signatureTeleportExemptMs;
    private boolean antibodyEnabled;
    private int antibodyMax;
    private double antibodyMatchThreshold;
    private boolean antibodyAutoLearn;
    private double antibodyLearnThreshold;
    private List<String> antibodyLearnFamilies;
    private boolean aiEnabled;
    private String baseUrl, model, apiKey;
    private int maxTokens, timeoutSeconds, intervalSeconds, cooldownSeconds;
    private double aiMinConfidence;
    private double aiMinSubmitConfidence;
    private int circuitBreakMinutes;
    private String dbUrl, dbUser, dbPassword, kaelorvynConfigFile, kaelorvynSchema;
    private int warningsBeforeBan, warningExpireDays;
    private int preKickDelaySeconds;
    private int aiPreKickDelaySeconds;
    private int punishCooldownSeconds;
    private boolean kick, ban, broadcastBan;
    private boolean requireAiConfirm;
    private String lobbyServer;
    private int banDurationHours;
    private boolean noticeEnabled;
    private boolean alertsEnabled;
    private double alertConfidence;
    private double alertsConfidence;
    private String alertsPrefix;
    private String kickTitle, kickSubtitle;
    private String kickActionbar;
    private List<String> kickScreen, banScreen, noticeLines;

    public Settings(KaelorvynGuard plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        YamlConfiguration c = (YamlConfiguration) plugin.getConfig();
        packetEnabled = c.getBoolean("engine.packet-enabled", true);
        activeDetectionEnabled = c.getBoolean("active-detection.enabled", false);
        flightEnabled = c.getBoolean("checks.flight.enabled", true);
        speedEnabled = c.getBoolean("checks.speed.enabled", true);
        noFallEnabled = c.getBoolean("checks.nofall.enabled", true);
        phaseEnabled = c.getBoolean("checks.phase.enabled", true);
        timerEnabled = c.getBoolean("checks.timer.enabled", true);
        reachEnabled = c.getBoolean("checks.reach.enabled", true);
        aimEnabled = c.getBoolean("checks.aim.enabled", true);
        clickerEnabled = c.getBoolean("checks.autoclicker.enabled", true);
        auraEnabled = c.getBoolean("checks.killaura.enabled", true);
        scaffoldEnabled = c.getBoolean("checks.scaffold.enabled", true);
        fastBreakEnabled = c.getBoolean("checks.fastbreak.enabled", true);
        multiBreakEnabled = c.getBoolean("checks.multibreak.enabled", true);
        elytraEnabled = c.getBoolean("checks.elytra.enabled", true);
        vehicleEnabled = c.getBoolean("checks.vehicle.enabled", true);
        climbEnabled = c.getBoolean("checks.climb.enabled", true);
        fastFallEnabled = c.getBoolean("checks.fastfall.enabled", true);
        blinkEnabled = c.getBoolean("checks.blink.enabled", false);
        multiTargetEnabled = c.getBoolean("checks.multitarget.enabled", true);
        noSwingEnabled = c.getBoolean("checks.noswing.enabled", true);
        knockbackEnabled = c.getBoolean("checks.knockback.enabled", true);
        fastPlaceEnabled = c.getBoolean("checks.fastplace.enabled", true);
        tickTimerEnabled = c.getBoolean("checks.ticktimer.enabled", true);
        aimSnapEnabled = c.getBoolean("checks.aimsnap.enabled", true);
        xrayEnabled = c.getBoolean("checks.xray.enabled", true);
        airJumpEnabled = c.getBoolean("checks.airjump.enabled", true);
        spiderEnabled = c.getBoolean("checks.spider.enabled", true);
        jesusEnabled = c.getBoolean("checks.jesus.enabled", true);
        noSlowdownEnabled = c.getBoolean("checks.noslowdown.enabled", true);
        criticalsEnabled = c.getBoolean("checks.criticals.enabled", false);
        throughWallsEnabled = c.getBoolean("checks.throughwalls.enabled", true);
        airPlaceEnabled = c.getBoolean("checks.airplace.enabled", true);
        instantMineEnabled = c.getBoolean("checks.instantmine.enabled", true);
        packetSpamEnabled = c.getBoolean("checks.packetspam.enabled", true);
        attackWhileMiningEnabled = c.getBoolean("checks.attackwhilemining.enabled", true);
        airLiquidBreakEnabled = c.getBoolean("checks.airliquidbreak.enabled", true);
        animationSpamEnabled = c.getBoolean("checks.animationspam.enabled", true);
        crashEnabled = c.getBoolean("checks.crash.enabled", true);
        chatEnabled = c.getBoolean("checks.chat.enabled", true);
        chatNewlineEnabled = c.getBoolean("checks.chat.newline", true);
        backtrackEnabled = c.getBoolean("checks.backtrack.enabled", true);
        attackRecheckEnabled = c.getBoolean("checks.attackrecheck.enabled", true);
        wrongToolEnabled = c.getBoolean("checks.wrongtool.enabled", true);
        multiPlaceEnabled = c.getBoolean("checks.multiplace.enabled", true);
        lookPacketsEnabled = c.getBoolean("checks.lookpackets.enabled", true);
        slowFallEnabled = c.getBoolean("checks.slowfall.enabled", true);
        stepEnabled = c.getBoolean("checks.step.enabled", true);
        trustEnabled = c.getBoolean("trust.enabled", true);
        mitigationEnabled = c.getBoolean("mitigation.enabled", true);
        minAirTicks = c.getInt("checks.flight.min-air-ticks", 15);
        maxUpSpeed = c.getDouble("checks.flight.max-up-speed", 0.66);
        speedTolerance = c.getDouble("checks.speed.tolerance", 1.9);
        maxFallDistance = c.getDouble("checks.nofall.max-fall-distance", 3.0);
        timerDriftMs = c.getLong("checks.timer.drift-ms", 120);
        burstPackets = c.getInt("checks.timer.burst-packets", 15);
        baseReach = c.getDouble("checks.reach.base-reach", 3.0);
        pingFactor = c.getDouble("checks.reach.ping-factor", 0.0028);
        pingCap = c.getDouble("checks.reach.ping-cap", 0.55);
        reachMargin = c.getDouble("checks.reach.margin", 0.03);
        aimMinSamples = c.getInt("checks.aim.min-samples", 40);
        clickerMinSamples = c.getInt("checks.autoclicker.min-samples", 25);
        clickerMaxCps = c.getDouble("checks.autoclicker.max-cps", 20);
        auraMaxAngle = c.getDouble("checks.killaura.max-angle", 90);
        scaffoldMaxPlaces = c.getInt("checks.scaffold.max-places-per-second", 12);
        fastBreakRatio = c.getDouble("checks.fastbreak.min-ratio", 0.55);
        fastBreakMinExpectedMs = c.getLong("checks.fastbreak.min-expected-ms", 500);
        multiBreakMax = c.getInt("checks.multibreak.max-breaks-per-tick", 2);
        elytraMaxSpeed = c.getDouble("checks.elytra.max-speed", 2.6);
        vehicleMaxSpeed = c.getDouble("checks.vehicle.max-speed", 5.0);
        climbMaxSpeed = c.getDouble("checks.climb.max-speed", 0.2);
        fastFallMax = c.getDouble("checks.fastfall.max-speed", 4.2);
        fastFallMaxIntervalMs = c.getInt("checks.fastfall.max-interval-ms", 120);
        fastFallMinAirTicks = c.getInt("checks.fastfall.min-air-ticks", 5);
        blinkMinSilenceMs = c.getLong("checks.blink.min-silence-ms", 1000);
        multiTargetMax = c.getInt("checks.multitarget.max-targets", 4);
        noSwingAttackThreshold = c.getInt("checks.noswing.attack-threshold", 5);
        noSwingBreakMs = c.getLong("checks.noswing.break-ms", 600);
        noSwingAttackWindowMs = c.getLong("checks.noswing.attack-window-ms", 1000);
        knockbackWindowMs = c.getLong("checks.knockback.window-ms", 400);
        knockbackTimeoutMs = c.getLong("checks.knockback.timeout-ms", 2000);
        fastPlaceMax = c.getInt("checks.fastplace.max-places", 20);
        placeDigWindowMs = c.getLong("checks.place.dig-window-ms", 100);
        placeWindowMs = c.getLong("checks.place.window-ms", 1000);
        scaffoldNearDistance = c.getDouble("checks.scaffold.near-distance", 2.5);
        tickEndPackets = c.getInt("checks.ticktimer.max-flying-packets", 2);
        snapYaw = c.getInt("checks.aimsnap.yaw", 320);
        snapPrior = c.getInt("checks.aimsnap.prior", 30);
        oreRatio = c.getDouble("checks.xray.ore-ratio", 0.8);
        minOreSamples = c.getInt("checks.xray.min-samples", 40);
        flightAscentMinTicks = c.getInt("checks.flight.ascent-min-ticks", 6);
        flightAscentMinDy = c.getDouble("checks.flight.ascent-min-dy", 0.03);
        flightAscentResetDy = c.getDouble("checks.flight.ascent-reset-dy", -0.08);
        flightAirMoveExtra = c.getInt("checks.flight.air-move-extra-ticks", 15);
        flightHoverExtra = c.getInt("checks.flight.hover-extra-ticks", 20);
        flightAirMoveMinSpeed = c.getDouble("checks.flight.air-move-min-speed", 0.05);
        flightAirMoveMinDy = c.getDouble("checks.flight.air-move-min-dy", -0.05);
        flightUpRatio = c.getDouble("checks.flight.up-over-jump-ratio", 1.15);
        flightHoverMaxDy = c.getDouble("checks.flight.hover-max-dy", 0.02);
        flightEmitCooldownMs = c.getLong("checks.flight.emit-cooldown-ms", 5000);
        speedIntervalMaxMs = c.getLong("checks.speed.interval-max-ms", 120);
        speedAirMultiplier = c.getDouble("checks.speed.air-multiplier", 1.6);
        speedMinHorizontal = c.getDouble("checks.speed.min-horizontal", 0.6);
        slowFallMinAirTicks = c.getInt("checks.slowfall.min-air-ticks", 30);
        slowFallMinTicks = Math.max(1, c.getInt("checks.slowfall.min-consecutive-ticks", 4));
        slowFallMaxDy = c.getDouble("checks.slowfall.max-dy", -0.05);
        slowFallMinDy = c.getDouble("checks.slowfall.min-dy", -0.15);
        slowFallWaterExitGraceMs = c.getLong("checks.slowfall.water-exit-grace-ms", 3000);
        slowFallEmitCooldownMs = c.getLong("checks.slowfall.emit-cooldown-ms", 1500);
        stepMaxIntervalMs = c.getInt("checks.step.max-interval-ms", 10);
        stepMinDy = c.getDouble("checks.step.min-dy", 0.1);
        stepMinSum = c.getDouble("checks.step.min-sum", 0.6);
        stepRequireGround = c.getBoolean("checks.step.require-on-ground", true);
        spiderMaxHorizontal = c.getDouble("checks.spider.max-horizontal", 0.4);
        spiderMinDy = c.getDouble("checks.spider.min-dy", 0.15);
        spiderMaxDy = c.getDouble("checks.spider.max-dy", 0.4);
        jesusMaxDy = c.getDouble("checks.jesus.max-dy", 0.05);
        jesusPulses = c.getInt("checks.jesus.pulses", 2);
        jesusPulseWindowMs = c.getLong("checks.jesus.pulse-window-ms", 2000);
        jesusJumpRatio = c.getDouble("checks.jesus.jump-ratio", 0.7);
        timerBalanceThreshold = c.getInt("checks.timer.balance-threshold", 5);
        timerMinIntervalMs = c.getLong("checks.timer.min-interval-ms", 5);
        badPacketsMaxAttacks = c.getInt("checks.badpackets.max-attacks-per-tick", 1);
        badPacketsMaxSwings = c.getInt("checks.badpackets.max-swings-per-tick", 2);
        badPacketsMaxClicks = c.getInt("checks.badpackets.max-clicks-per-tick", 8);
        signaturePacketFlyMinDy = c.getDouble("signatures.packetfly.min-dy", 400);
        signatureDualTolerance = c.getDouble("signatures.dual-move.tolerance", 0.001);
        signatureMeteorTolerance = c.getDouble("signatures.meteor-anti-kick.tolerance", 0.0005);
        signaturePacketFlyEnabled = c.getBoolean("signatures.packetfly.enabled", true);
        signatureDualMoveEnabled = c.getBoolean("signatures.dual-move.enabled", true);
        signatureMeteorEnabled = c.getBoolean("signatures.meteor-anti-kick.enabled", true);
        signaturePacketFlyConfidence = c.getDouble("signatures.packetfly.confidence", 0.98);
        signatureDualMoveConfidence = c.getDouble("signatures.dual-move.confidence", 0.95);
        signatureMeteorConfidence = c.getDouble("signatures.meteor-anti-kick.confidence", 0.9);
        signatureDualMoveDy = c.getDouble("signatures.dual-move.dy", -0.01);
        signatureMeteorDy = c.getDouble("signatures.meteor-anti-kick.dy", -0.0313);
        signatureMeteorMinHits = Math.max(2, c.getInt("signatures.meteor-anti-kick.min-hits", 3));
        signatureMeteorWindowMs = c.getLong("signatures.meteor-anti-kick.window-ms", 2000);
        signatureDualMinHits = Math.max(2, c.getInt("signatures.dual-move.min-hits", 3));
        signatureDualWindowMs = c.getLong("signatures.dual-move.window-ms", 2000);
        signatureTeleportExemptMs = c.getLong("signatures.teleport-exempt-ms", 1500);
        double autoLearnMin = c.getDouble("antibody.auto-learn-min-confidence", 0.95);
        if (c.contains("antibody.min-confirm-confidence")) {
            autoLearnMin = c.getDouble("antibody.min-confirm-confidence", autoLearnMin);
        }
        antibodyLearnMin = autoLearnMin;
        antibodyMax = Math.max(1, c.getInt("antibody.max-antibodies", 200));
        auraBotEnabled = c.getBoolean("aurabot.enabled", true);
        auraBotCooldownMs = c.getLong("aurabot.cooldown-ms", 5000);
        auraBotDistance = c.getDouble("aurabot.distance", 1.5);
        auraBotYOffset = c.getDouble("aurabot.y-offset", 1.0);
        auraBotLifetimeTicks = c.getInt("aurabot.lifetime-ticks", 5);
        blinkAttackRecentMs = c.getLong("checks.blink.attack-recent-ms", 3000);
        blinkEmitCooldownMs = c.getLong("checks.blink.emit-cooldown-ms", 10000);
        knockbackMinRatio = c.getDouble("checks.knockback.min-ratio", 0.3);
        knockbackMinKbSpeed = c.getDouble("checks.knockback.min-kb-speed", 0.01);
        knockbackMinMove = c.getDouble("checks.knockback.min-move", 0.005);
        knockbackWeakTicks = c.getInt("checks.knockback.weak-ticks", 3);
        clickerEntropyMax = c.getDouble("checks.autoclicker.entropy-max", 2.2);
        clickerMinIntervalMs = c.getLong("checks.autoclicker.min-interval-ms", 10);
        clickerBinMs = c.getInt("checks.autoclicker.bin-ms", 5);
        clickerWindowMs = c.getLong("checks.autoclicker.window-ms", 1000);
        aimGcdMax = c.getDouble("checks.aim.gcd-max", 0.001);
        aimStdMax = c.getDouble("checks.aim.std-max", 0.8);
        airJumpMinAirTicks = c.getInt("checks.airjump.min-air-ticks", 5);
        airJumpRatio = c.getDouble("checks.airjump.jump-ratio", 0.9);
        airJumpPulseWindowMs = c.getLong("checks.airjump.pulse-window-ms", 2000);
        criticalsWindowMs = c.getInt("checks.criticals.window-ms", 100);
        criticalsMaxHorizontal = c.getDouble("checks.criticals.max-horizontal", 0.1);
        criticalDamageEnabled = c.getBoolean("checks.criticals.damage-enabled", true);
        criticalsTinyMaxDy = c.getDouble("checks.criticals.tiny-max-dy", 0.05);
        autoSwitchWindowMs = c.getLong("checks.autoswitch.window-ms", 50);
        autoSwitchEmitEvery = Math.max(2, c.getInt("checks.autoswitch.emit-every", 3));
        phaseOnlyFullBlocks = c.getBoolean("checks.phase.only-full-blocks", true);
        exemptionTeleportMs = c.getLong("exemptions.teleport-ms", 1500);
        exemptionRespawnMs = c.getLong("exemptions.respawn-ms", 2000);
        exemptionVelocityMs = c.getLong("exemptions.velocity-ms", 800);
        joinGraceMs = c.getLong("exemptions.join-grace-ms", 3000);
        teleportJumpBlocks = c.getDouble("exemptions.teleport-jump-blocks", 12.0);
        airJumpPulses = c.getInt("checks.airjump.pulses", 2);
        spiderTicks = c.getInt("checks.spider.ticks", 8);
        jesusAirTicks = c.getInt("checks.jesus.air-ticks", 10);
        noSlowdownRatio = c.getDouble("checks.noslowdown.ratio", 0.8);
        criticalsMoves = c.getInt("checks.criticals.moves", 2);
        instantMineMs = c.getInt("checks.instantmine.ms", 100);
        instantMineMinExpectedMs = c.getLong("checks.instantmine.min-expected-ms", 1000);
        packetSpamMax = c.getInt("checks.packetspam.max", 20);
        clickerCvMax = c.getDouble("checks.autoclicker.cv-max", 0.15);
        clickerPerfectRepeat = c.getInt("checks.autoclicker.perfect-repeat", 8);
        elytraBoostMax = c.getDouble("checks.elytra.boost-max", 2.0);
        strictNoItem = c.getBoolean("checks.elytra.strict-no-item", false);
        legacyReachMargin = c.getDouble("checks.reach.legacy-margin", 0.1);
        reachThroughWallsMargin = c.getDouble("checks.reach.through-walls-margin", 0.1);
        attackWhileMiningMs = c.getInt("checks.attackwhilemining.ms", 100);
        animationSpamFactor = c.getInt("checks.animationspam.factor", 3);
        animationSpamMinSwings = c.getInt("checks.animationspam.min-swings", 30);
        animationSpamCheckEvery = c.getInt("checks.animationspam.check-every", 50);
        animationSpamExtraAllowance = c.getInt("checks.animationspam.extra-allowance", 20);
        crashWindowClicks = c.getInt("checks.crash.window-clicks", 40);
        windowClickWindowMs = c.getLong("checks.window.window-ms", 1000);
        windowPacketOrderMs = c.getLong("checks.window.packet-order-ms", 100);
        chatSpamPerSecond = c.getInt("checks.chat.spam-per-second", 5);
        chatWindowMs = c.getLong("checks.chat.window-ms", 1000);
        trustDefault = c.getInt("trust.default-level", 3);
        trustMin = c.getInt("trust.min-level", 1);
        mitigationMultiplier = c.getDouble("mitigation.damage-multiplier", 0.5);
        mitigationActivation = c.getDouble("mitigation.activation-confidence", 0.8);
        List<String> cats = c.getStringList("mitigation.categories");
        if (cats == null || cats.isEmpty()) {
            cats = List.of("AIM", "AUTO_CLICKER", "AUTO_CLICKER_RIGHT", "KILL_AURA", "REACH",
                    "ANTIBODY", "PACKET_FLY", "CRITICALS", "THROUGH_WALLS");
        }
        mitigationCategories = List.copyOf(cats);
        backtrackZScore = c.getDouble("checks.backtrack.z-score", 4.5);
        backtrackMinSamples = c.getInt("checks.backtrack.min-samples", 20);
        backtrackMinStd = c.getDouble("checks.backtrack.min-std", 1.0);
        antibodyScanEveryMoves = Math.max(1, c.getInt("checks.scan.antibody-every-moves", 100));
        backtrackScanEveryMoves = Math.max(1, c.getInt("checks.scan.backtrack-every-moves", 50));
        pingSamples = Math.max(10, c.getInt("checks.scan.ping-samples", 100));
        hitboxHistoryMs = c.getLong("checks.scan.hitbox-history-ms", 1500);
        hitboxMaxSnapshots = c.getInt("checks.scan.hitbox-max-snapshots", 120);
        targetWindowMs = c.getLong("checks.scan.target-window-ms", 1000);
        wrongToolRatio = c.getDouble("checks.wrongtool.ratio", 0.4);
        wrongToolHandFactor = c.getDouble("checks.wrongtool.hand-factor", 1.5);
        multiPlaceMax = c.getInt("checks.multiplace.max", 3);
        lookPacketsMax = c.getInt("checks.lookpackets.max", 5);
        signaturesEnabled = c.getBoolean("signatures.enabled", true);
        antibodyEnabled = c.getBoolean("antibody.enabled", true);
        antibodyMatchThreshold = c.getDouble("antibody.match-threshold", 0.85);
        antibodyAutoLearn = c.getBoolean("antibody.auto-learn", true);
        antibodyLearnThreshold = c.getDouble("antibody.learn-threshold", 0.8);
        List<String> fams = c.getStringList("antibody.auto-learn-families");
        if (fams == null || fams.isEmpty()) {
            fams = List.of("packetfly-y420", "aura-bot",
                    "wurstb-flight", "wurstb-speed", "wurstb-packetfly",
                    "wurstb-nofall", "wurstb-step", "wurstb-spider",
                    "wurstb-jesus", "wurstb-dolphin", "wurstb-airjump",
                    "wurstb-criticals", "wurstb-reach", "wurstb-killaura",
                    "wurstb-scaffold", "wurstb-nuker", "wurstb-fastbreak",
                    "wurstb-fastplace", "wurstb-fastladder", "wurstb-noslowdown",
                    "wurstb-novelocity", "wurstb-blink", "wurstb-timer",
                    "wurstb-boatfly", "wurstb-elytrafly", "wurstb-jetpack",
                    "wurstb-glide", "wurstb-highjump", "wurstb-bunnyhop",
                    "wurst-client-criticals");
        }
        antibodyLearnFamilies = List.copyOf(fams);
        aiEnabled = c.getBoolean("ai.enabled", true);
        baseUrl = c.getString("ai.base-url", "https://api.agnes-ai.cn");
        model = c.getString("ai.model", "agnes-2.5-flash");
        apiKey = c.getString("ai.api-key", "");
        if (apiKey == null || apiKey.isBlank()) apiKey = System.getenv("AGNES_API_KEY");
        if (apiKey == null) apiKey = "";
        maxTokens = c.getInt("ai.max-tokens", 1200);
        timeoutSeconds = c.getInt("ai.timeout-seconds", 60);
        intervalSeconds = Math.max(10, c.getInt("ai.interval-seconds", 60));
        cooldownSeconds = c.getInt("ai.cooldown-seconds", 10);
        aiMinConfidence = c.getDouble("ai.min-confidence", 0.8);
        aiMinSubmitConfidence = c.getDouble("ai.min-submit-confidence", 0.3);
        circuitBreakMinutes = c.getInt("ai.circuit-break-minutes", 5);
        dbUrl = c.getString("storage.database.url", "");
        dbUser = c.getString("storage.database.user", "");
        dbPassword = c.getString("storage.database.password", "");
        kaelorvynConfigFile = c.getString("storage.kaelorvynban.config-file", "");
        kaelorvynSchema = c.getString("storage.kaelorvynban.schema", "kaerban");
        warningsBeforeBan = Math.max(1, c.getInt("punish.warnings-before-ban", 3));
        warningExpireDays = c.getInt("punish.warning-expire-days", 7);
        preKickDelaySeconds = Math.max(0, c.getInt("punish.pre-kick-delay-seconds", 2));
        aiPreKickDelaySeconds = Math.max(0, c.getInt("punish.ai-pre-kick-delay-seconds", 5));
        punishCooldownSeconds = Math.max(0, c.getInt("punish.cooldown-seconds", 30));
        kick = c.getBoolean("punish.kick", false);
        requireAiConfirm = c.getBoolean("punish.require-ai-confirm", true);
        ban = c.getBoolean("punish.ban", false);
        broadcastBan = c.getBoolean("punish.broadcast-ban", true);
        lobbyServer = c.getString("punish.lobby-server", "lobby");
        banDurationHours = c.getInt("punish.ban-duration-hours", 168);
        noticeEnabled = c.getBoolean("notice.enabled", true);
        alertsEnabled = c.getBoolean("alerts.enabled", true);
        alertsConfidence = c.getDouble("alerts.confidence", 0.7);
        alertConfidence = alertsConfidence;
        alertsPrefix = c.getString("alerts.prefix", "&8[&bKaelorvynGuard&8]");
        kickTitle = c.getString("punish.messages.kick-title", "Kaelorvyn 安全中心");
        kickSubtitle = c.getString("punish.messages.kick-subtitle", "检测到可疑行为：{reason}");
        kickActionbar = c.getString("punish.messages.kick-actionbar",
                "&c&l第 {count} 次警告，还剩 {left} 次将被封禁");
        kickScreen = c.getStringList("punish.messages.kick-screen");
        banScreen = c.getStringList("punish.messages.ban-screen");
        noticeLines = c.getStringList("notice.message");
    }

    public boolean packetEnabled() { return packetEnabled; }
    public boolean activeDetectionEnabled() { return activeDetectionEnabled; }
    public boolean flightEnabled() { return flightEnabled; }
    public boolean speedEnabled() { return speedEnabled; }
    public boolean noFallEnabled() { return noFallEnabled; }
    public boolean phaseEnabled() { return phaseEnabled; }
    public boolean timerEnabled() { return timerEnabled; }
    public boolean reachEnabled() { return reachEnabled; }
    public boolean aimEnabled() { return aimEnabled; }
    public boolean clickerEnabled() { return clickerEnabled; }
    public boolean auraEnabled() { return auraEnabled; }
    public boolean scaffoldEnabled() { return scaffoldEnabled; }
    public boolean fastBreakEnabled() { return fastBreakEnabled; }
    public boolean multiBreakEnabled() { return multiBreakEnabled; }
    public int minAirTicks() { return minAirTicks; }
    public double maxUpSpeed() { return maxUpSpeed; }
    public double speedTolerance() { return speedTolerance; }
    public double maxFallDistance() { return maxFallDistance; }
    public long timerDriftMs() { return timerDriftMs; }
    public int burstPackets() { return burstPackets; }
    public double baseReach() { return baseReach; }
    public double pingFactor() { return pingFactor; }
    public double pingCap() { return pingCap; }
    public double reachMargin() { return reachMargin; }
    public int aimMinSamples() { return aimMinSamples; }
    public int clickerMinSamples() { return clickerMinSamples; }
    public double clickerMaxCps() { return clickerMaxCps; }
    public double auraMaxAngle() { return auraMaxAngle; }
    public int scaffoldMaxPlaces() { return scaffoldMaxPlaces; }
    public double fastBreakRatio() { return fastBreakRatio; }
    public int multiBreakMax() { return multiBreakMax; }
    public boolean elytraEnabled() { return elytraEnabled; }
    public boolean vehicleEnabled() { return vehicleEnabled; }
    public boolean climbEnabled() { return climbEnabled; }
    public boolean fastFallEnabled() { return fastFallEnabled; }
    public boolean blinkEnabled() { return blinkEnabled; }
    public boolean multiTargetEnabled() { return multiTargetEnabled; }
    public boolean noSwingEnabled() { return noSwingEnabled; }
    public boolean knockbackEnabled() { return knockbackEnabled; }
    public boolean fastPlaceEnabled() { return fastPlaceEnabled; }
    public boolean tickTimerEnabled() { return tickTimerEnabled; }
    public boolean aimSnapEnabled() { return aimSnapEnabled; }
    public boolean xrayEnabled() { return xrayEnabled; }
    public double elytraMaxSpeed() { return elytraMaxSpeed; }
    public double vehicleMaxSpeed() { return vehicleMaxSpeed; }
    public double climbMaxSpeed() { return climbMaxSpeed; }
    public double fastFallMax() { return fastFallMax; }
    public long blinkMinSilenceMs() { return blinkMinSilenceMs; }
    public int multiTargetMax() { return multiTargetMax; }
    public int noSwingAttackThreshold() { return noSwingAttackThreshold; }
    public long noSwingBreakMs() { return noSwingBreakMs; }
    public long knockbackWindowMs() { return knockbackWindowMs; }
    public int fastPlaceMax() { return fastPlaceMax; }
    public int tickEndPackets() { return tickEndPackets; }
    public int snapYaw() { return snapYaw; }
    public int snapPrior() { return snapPrior; }
    public double oreRatio() { return oreRatio; }
    public int minOreSamples() { return minOreSamples; }
    public int flightAscentMinTicks() { return flightAscentMinTicks; }
    public double flightAscentMinDy() { return flightAscentMinDy; }
    public int flightAirMoveExtra() { return flightAirMoveExtra; }
    public int flightHoverExtra() { return flightHoverExtra; }
    public double flightAirMoveMinSpeed() { return flightAirMoveMinSpeed; }
    public double flightAirMoveMinDy() { return flightAirMoveMinDy; }
    public double flightUpRatio() { return flightUpRatio; }
    public double flightHoverMaxDy() { return flightHoverMaxDy; }
    public long flightEmitCooldownMs() { return flightEmitCooldownMs; }
    public long speedIntervalMaxMs() { return speedIntervalMaxMs; }
    public double speedAirMultiplier() { return speedAirMultiplier; }
    public double speedMinHorizontal() { return speedMinHorizontal; }
    public int slowFallMinAirTicks() { return slowFallMinAirTicks; }
    public int slowFallMinTicks() { return slowFallMinTicks; }
    public double slowFallMaxDy() { return slowFallMaxDy; }
    public double slowFallMinDy() { return slowFallMinDy; }
    public long slowFallWaterExitGraceMs() { return slowFallWaterExitGraceMs; }
    public long slowFallEmitCooldownMs() { return slowFallEmitCooldownMs; }
    public int stepMaxIntervalMs() { return stepMaxIntervalMs; }
    public double stepMinDy() { return stepMinDy; }
    public double stepMinSum() { return stepMinSum; }
    public boolean stepRequireGround() { return stepRequireGround; }
    public double spiderMaxHorizontal() { return spiderMaxHorizontal; }
    public double spiderMinDy() { return spiderMinDy; }
    public double spiderMaxDy() { return spiderMaxDy; }
    public double jesusMaxDy() { return jesusMaxDy; }
    public int jesusPulses() { return jesusPulses; }
    public long jesusPulseWindowMs() { return jesusPulseWindowMs; }
    public double jesusJumpRatio() { return jesusJumpRatio; }
    public int timerBalanceThreshold() { return timerBalanceThreshold; }
    public long timerMinIntervalMs() { return timerMinIntervalMs; }
    public int badPacketsMaxAttacks() { return badPacketsMaxAttacks; }
    public int badPacketsMaxSwings() { return badPacketsMaxSwings; }
    public int badPacketsMaxClicks() { return badPacketsMaxClicks; }
    public double signaturePacketFlyMinDy() { return signaturePacketFlyMinDy; }
    public double signatureDualTolerance() { return signatureDualTolerance; }
    public double signatureMeteorTolerance() { return signatureMeteorTolerance; }
    public boolean signaturePacketFlyEnabled() { return signaturePacketFlyEnabled; }
    public boolean signatureDualMoveEnabled() { return signatureDualMoveEnabled; }
    public boolean signatureMeteorEnabled() { return signatureMeteorEnabled; }
    public double signaturePacketFlyConfidence() { return signaturePacketFlyConfidence; }
    public double signatureDualMoveConfidence() { return signatureDualMoveConfidence; }
    public double signatureMeteorConfidence() { return signatureMeteorConfidence; }
    public double signatureDualMoveDy() { return signatureDualMoveDy; }
    public double signatureMeteorDy() { return signatureMeteorDy; }
    public int signatureMeteorMinHits() { return signatureMeteorMinHits; }
    public long signatureMeteorWindowMs() { return signatureMeteorWindowMs; }
    public int signatureDualMinHits() { return signatureDualMinHits; }
    public long signatureDualWindowMs() { return signatureDualWindowMs; }
    public long signatureTeleportExemptMs() { return signatureTeleportExemptMs; }
    public double antibodyLearnMin() { return antibodyLearnMin; }
    public boolean auraBotEnabled() { return auraBotEnabled; }
    public long auraBotCooldownMs() { return auraBotCooldownMs; }
    public double auraBotDistance() { return auraBotDistance; }
    public double auraBotYOffset() { return auraBotYOffset; }
    public int auraBotLifetimeTicks() { return auraBotLifetimeTicks; }
    public long blinkAttackRecentMs() { return blinkAttackRecentMs; }
    public long blinkEmitCooldownMs() { return blinkEmitCooldownMs; }
    public double knockbackMinRatio() { return knockbackMinRatio; }
    public double knockbackMinKbSpeed() { return knockbackMinKbSpeed; }
    public double knockbackMinMove() { return knockbackMinMove; }
    public int knockbackWeakTicks() { return knockbackWeakTicks; }
    public double clickerEntropyMax() { return clickerEntropyMax; }
    public long clickerMinIntervalMs() { return clickerMinIntervalMs; }
    public int clickerBinMs() { return clickerBinMs; }
    public double aimGcdMax() { return aimGcdMax; }
    public double aimStdMax() { return aimStdMax; }
    public int airJumpMinAirTicks() { return airJumpMinAirTicks; }
    public double airJumpRatio() { return airJumpRatio; }
    public int criticalsWindowMs() { return criticalsWindowMs; }
    public double criticalsMaxHorizontal() { return criticalsMaxHorizontal; }
    public boolean criticalDamageEnabled() { return criticalDamageEnabled; }
    public boolean phaseOnlyFullBlocks() { return phaseOnlyFullBlocks; }
    public long exemptionTeleportMs() { return exemptionTeleportMs; }
    public long exemptionRespawnMs() { return exemptionRespawnMs; }
    public long exemptionVelocityMs() { return exemptionVelocityMs; }
    public long joinGraceMs() { return joinGraceMs; }
    public double teleportJumpBlocks() { return teleportJumpBlocks; }
    public boolean airJumpEnabled() { return airJumpEnabled; }
    public boolean spiderEnabled() { return spiderEnabled; }
    public boolean jesusEnabled() { return jesusEnabled; }
    public boolean noSlowdownEnabled() { return noSlowdownEnabled; }
    public boolean criticalsEnabled() { return criticalsEnabled; }
    public boolean throughWallsEnabled() { return throughWallsEnabled; }
    public boolean airPlaceEnabled() { return airPlaceEnabled; }
    public boolean instantMineEnabled() { return instantMineEnabled; }
    public boolean packetSpamEnabled() { return packetSpamEnabled; }
    public int airJumpPulses() { return airJumpPulses; }
    public int spiderTicks() { return spiderTicks; }
    public int jesusAirTicks() { return jesusAirTicks; }
    public double noSlowdownRatio() { return noSlowdownRatio; }
    public int criticalsMoves() { return criticalsMoves; }
    public int instantMineMs() { return instantMineMs; }
    public int packetSpamMax() { return packetSpamMax; }
    public boolean attackWhileMiningEnabled() { return attackWhileMiningEnabled; }
    public boolean airLiquidBreakEnabled() { return airLiquidBreakEnabled; }
    public boolean animationSpamEnabled() { return animationSpamEnabled; }
    public boolean crashEnabled() { return crashEnabled; }
    public boolean chatEnabled() { return chatEnabled; }
    public boolean chatNewlineEnabled() { return chatNewlineEnabled; }
    public double clickerCvMax() { return clickerCvMax; }
    public double elytraBoostMax() { return elytraBoostMax; }
    public boolean strictNoItem() { return strictNoItem; }
    public double legacyReachMargin() { return legacyReachMargin; }
    public int clickerPerfectRepeat() { return clickerPerfectRepeat; }
    public int attackWhileMiningMs() { return attackWhileMiningMs; }
    public int animationSpamFactor() { return animationSpamFactor; }
    public int crashWindowClicks() { return crashWindowClicks; }
    public int chatSpamPerSecond() { return chatSpamPerSecond; }
    public long chatWindowMs() { return chatWindowMs; }
    public boolean trustEnabled() { return trustEnabled; }
    public boolean mitigationEnabled() { return mitigationEnabled; }
    public boolean backtrackEnabled() { return backtrackEnabled; }
    public int trustDefault() { return trustDefault; }
    public int trustMin() { return trustMin; }
    public double mitigationMultiplier() { return mitigationMultiplier; }
    public double mitigationActivation() { return mitigationActivation; }
    public List<String> mitigationCategories() { return mitigationCategories; }
    public double backtrackZScore() { return backtrackZScore; }
    public boolean attackRecheckEnabled() { return attackRecheckEnabled; }
    public boolean wrongToolEnabled() { return wrongToolEnabled; }
    public boolean multiPlaceEnabled() { return multiPlaceEnabled; }
    public boolean lookPacketsEnabled() { return lookPacketsEnabled; }
    public double wrongToolRatio() { return wrongToolRatio; }
    public int multiPlaceMax() { return multiPlaceMax; }
    public int lookPacketsMax() { return lookPacketsMax; }
    public boolean slowFallEnabled() { return slowFallEnabled; }
    public boolean stepEnabled() { return stepEnabled; }
    public int attackRecheckDelayMs() { return attackRecheckDelayMs; }
    public double flightAscentResetDy() { return flightAscentResetDy; }
    public int fastFallMaxIntervalMs() { return fastFallMaxIntervalMs; }
    public int fastFallMinAirTicks() { return fastFallMinAirTicks; }
    public long knockbackTimeoutMs() { return knockbackTimeoutMs; }
    public long airJumpPulseWindowMs() { return airJumpPulseWindowMs; }
    public double reachThroughWallsMargin() { return reachThroughWallsMargin; }
    public long clickerWindowMs() { return clickerWindowMs; }
    public int animationSpamMinSwings() { return animationSpamMinSwings; }
    public int animationSpamCheckEvery() { return animationSpamCheckEvery; }
    public int animationSpamExtraAllowance() { return animationSpamExtraAllowance; }
    public long placeDigWindowMs() { return placeDigWindowMs; }
    public long placeWindowMs() { return placeWindowMs; }
    public double scaffoldNearDistance() { return scaffoldNearDistance; }
    public long fastBreakMinExpectedMs() { return fastBreakMinExpectedMs; }
    public long instantMineMinExpectedMs() { return instantMineMinExpectedMs; }
    public double wrongToolHandFactor() { return wrongToolHandFactor; }
    public long windowClickWindowMs() { return windowClickWindowMs; }
    public long windowPacketOrderMs() { return windowPacketOrderMs; }
    public int backtrackMinSamples() { return backtrackMinSamples; }
    public double backtrackMinStd() { return backtrackMinStd; }
    public int antibodyScanEveryMoves() { return antibodyScanEveryMoves; }
    public int backtrackScanEveryMoves() { return backtrackScanEveryMoves; }
    public int pingSamples() { return pingSamples; }
    public int hitboxMaxSnapshots() { return hitboxMaxSnapshots; }
    public long hitboxHistoryMs() { return hitboxHistoryMs; }
    public long targetWindowMs() { return targetWindowMs; }
    public double criticalsTinyMaxDy() { return criticalsTinyMaxDy; }
    public long noSwingAttackWindowMs() { return noSwingAttackWindowMs; }
    public long autoSwitchWindowMs() { return autoSwitchWindowMs; }
    public int autoSwitchEmitEvery() { return autoSwitchEmitEvery; }

    // Intave TrustFactor 思路：信任越低，同样证据越容易被触发
    public double trustMultiplier(int level) {
        return switch (Math.max(0, Math.min(5, level))) {
            case 5 -> 0.5;
            case 4 -> 0.75;
            case 3 -> 1.0;
            case 2 -> 1.15;
            case 1 -> 1.3;
            default -> 1.5;
        };
    }
    public boolean signaturesEnabled() { return signaturesEnabled; }
    public boolean antibodyEnabled() { return antibodyEnabled; }
    public int antibodyMax() { return antibodyMax; }
    public double antibodyMatchThreshold() { return antibodyMatchThreshold; }
    public boolean antibodyAutoLearn() { return antibodyAutoLearn; }
    public double antibodyLearnThreshold() { return antibodyLearnThreshold; }
    public List<String> antibodyLearnFamilies() { return antibodyLearnFamilies; }
    public boolean aiEnabled() { return aiEnabled; }
    public String baseUrl() { return baseUrl; }
    public String model() { return model; }
    public String apiKey() { return apiKey; }
    public int maxTokens() { return maxTokens; }
    public int timeoutSeconds() { return timeoutSeconds; }
    public int intervalSeconds() { return intervalSeconds; }
    public int cooldownSeconds() { return cooldownSeconds; }
    public double aiMinConfidence() { return aiMinConfidence; }
    public double aiMinSubmitConfidence() { return aiMinSubmitConfidence; }
    public int circuitBreakMinutes() { return circuitBreakMinutes; }
    public String dbUrl() { return dbUrl; }
    public String dbUser() { return dbUser; }
    public String dbPassword() { return dbPassword; }
    public String kaelorvynConfigFile() { return kaelorvynConfigFile; }
    public String kaelorvynSchema() { return kaelorvynSchema; }
    public int warningsBeforeBan() { return warningsBeforeBan; }
    public int warningExpireDays() { return warningExpireDays; }
    public int preKickDelaySeconds() { return preKickDelaySeconds; }
    public int aiPreKickDelaySeconds() { return aiPreKickDelaySeconds; }
    public int punishCooldownSeconds() { return punishCooldownSeconds; }
    public boolean kick() { return kick; }
    public boolean requireAiConfirm() { return requireAiConfirm; }
    public boolean ban() { return ban; }
    public boolean broadcastBan() { return broadcastBan; }
    public String lobbyServer() { return lobbyServer; }
    public int banDurationHours() { return banDurationHours; }
    public boolean noticeEnabled() { return noticeEnabled; }
    public boolean alertsEnabled() { return alertsEnabled; }
    public double alertConfidence() { return alertConfidence; }
    public String alertsPrefix() { return alertsPrefix; }
    public String kickTitle() { return kickTitle; }
    public String kickSubtitle() { return kickSubtitle; }
    public String kickActionbar() { return kickActionbar; }
    public List<String> kickScreen() { return kickScreen; }
    public List<String> banScreen() { return banScreen; }
    public List<String> noticeLines() { return noticeLines; }
}
