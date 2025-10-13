package io.github.sequentialentropy.config;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

public class Rules {
    public static GameRules.Key<GameRules.IntRule> SAMPLING_DISTANCE;
    public static GameRules.Key<GameRules.IntRule> STRAIGHTNESS_PRECHECK_DISTANCE;
    public static void init() {
        // Maximum distance (in blocks) used to sample rail points for curve smoothing.
        SAMPLING_DISTANCE = GameRuleRegistry.register(
                "smoothMinecartsSamplingDistance",
                GameRules.Category.UPDATES,
                GameRuleFactory.createIntRule(6, 0)
        );

        // Number of consecutive blocks ahead that must form a valid straight path
        // before accepting a new velocity correction. Prevents jitter and derailment.
        STRAIGHTNESS_PRECHECK_DISTANCE = GameRuleRegistry.register(
                "smoothMinecartsStraightnessPrecheckDistance",
                GameRules.Category.UPDATES,
                GameRuleFactory.createIntRule(3, 0)
        );
    }
}
