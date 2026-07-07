package io.github.sequentialentropy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends VehicleEntity {
    public AbstractMinecartMixin(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }

    /**
     * When the minecart is noclipping inside a block when descending,
     * return the actual rail which would be one block above
     */
    @Inject(at = @At("RETURN"), method = "getCurrentBlockPosOrRailBelow()Lnet/minecraft/core/BlockPos;", cancellable = true)
    private void modifyGetCurrentBlockPosOrRailBelow(CallbackInfoReturnable<BlockPos> cir) {
        if (AbstractMinecart.useExperimentalMovement(this.level())) {
            BlockPos pos = cir.getReturnValue();
            BlockPos above = pos.above();
            if (
                    !this.level().getBlockState(pos).is(BlockTags.RAILS) &&
                    this.level().getBlockState(above).is(BlockTags.RAILS)
            ) {
                cir.setReturnValue(above);
            }
        }
    }
}
