package io.github.sequentialentropy.config;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;

public class Rules {
    public static GameRule<Integer> SAMPLING_DISTANCE;
    public static GameRule<Integer> STRAIGHTNESS_PRECHECK_DISTANCE;
    public static void init() {
        // Maximum distance (in blocks) used to sample rail points for curve smoothing.
        SAMPLING_DISTANCE = GameRuleBuilder.forInteger(6)
                .range(0, Integer.MAX_VALUE)
                .buildAndRegister(Identifier.fromNamespaceAndPath("smooth_minecarts", "sampling_distance"));

        // Number of consecutive blocks ahead that must form a valid straight path
        // before accepting a new velocity correction. Prevents jitter and derailment.
        STRAIGHTNESS_PRECHECK_DISTANCE = GameRuleBuilder.forInteger(3)
                .range(0, Integer.MAX_VALUE)
                .buildAndRegister(Identifier.fromNamespaceAndPath("smooth_minecarts", "straightness_precheck_distance"));
    }
}
