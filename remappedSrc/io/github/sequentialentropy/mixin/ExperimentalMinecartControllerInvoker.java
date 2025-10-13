package io.github.sequentialentropy.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ExperimentalMinecartController.class)
public interface ExperimentalMinecartControllerInvoker {
    @Invoker("setAngles")
    void invokeSetAngles(float yaw, float pitch);

    @Invoker("applySlopeVelocity")
    Vec3d invokeApplySlopeVelocity(Vec3d horizontalVelocity, RailShape railShape);

    @Invoker("applyInitialVelocity")
    Vec3d invokeApplyInitialVelocity(Vec3d horizontalVelocity);

    @Invoker("decelerateFromPoweredRail")
    Vec3d invokeDecelerateFromPoweredRail(Vec3d velocity, BlockState railState);

    @Invoker("accelerateFromPoweredRail")
    Vec3d invokeAccelerateFromPoweredRail(Vec3d velocity, BlockPos railPos, BlockState railState);
}
