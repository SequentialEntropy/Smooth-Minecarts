package io.github.sequentialentropy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NewMinecartBehavior.class)
public interface NewMinecartBehaviorInvoker {
    @Invoker("setRotation")
    void invokeSetRotation(float yaw, float pitch);

    @Invoker("calculateSlopeSpeed")
    Vec3 invokeCalculateSlopeSpeed(Vec3 horizontalVelocity, RailShape railShape);

    @Invoker("calculatePlayerInputSpeed")
    Vec3 invokeCalculatePlayerInputSpeed(Vec3 horizontalVelocity);

    @Invoker("calculateHaltTrackSpeed")
    Vec3 invokeCalculateHaltTrackSpeed(Vec3 velocity, BlockState railState);

    @Invoker("calculateBoostTrackSpeed")
    Vec3 invokeCalculateBoostTrackSpeed(Vec3 velocity, BlockPos railPos, BlockState railState);
}
