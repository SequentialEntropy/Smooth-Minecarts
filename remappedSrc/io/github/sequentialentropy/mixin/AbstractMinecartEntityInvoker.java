package io.github.sequentialentropy.mixin;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMinecartEntity.class)
public interface AbstractMinecartEntityInvoker {
    @Invoker("moveOffRail")
    void invokeMoveOffRail(ServerWorld world);

    @Invoker("applySlowdown")
    Vec3d invokeApplySlowdown(Vec3d velocity);

    @Invoker("getMaxSpeed")
    double invokeGetMaxSpeed(ServerWorld world);
}