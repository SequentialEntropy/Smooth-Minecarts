package io.github.sequentialentropy.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMinecart.class)
public interface AbstractMinecartInvoker {
    @Invoker("comeOffTrack")
    void invokeComeOffTrack(ServerLevel world);

    @Invoker("applyNaturalSlowdown")
    Vec3 invokeApplyNaturalSlowdown(Vec3 velocity);

    @Invoker("getMaxSpeed")
    double invokeGetMaxSpeed(ServerLevel world);
}