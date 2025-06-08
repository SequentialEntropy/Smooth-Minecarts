package io.github.sequentialentropy.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends VehicleEntity {
    public AbstractMinecartEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    /**
     * When the minecart is noclipping inside a block when descending,
     * return the actual rail which would be one block above
     */
    @Inject(at = @At("RETURN"), method = "getRailOrMinecartPos()Lnet/minecraft/util/math/BlockPos;", cancellable = true)
    private void modifyGetRailOrMinecartPos(CallbackInfoReturnable<BlockPos> cir) {
        if (AbstractMinecartEntity.areMinecartImprovementsEnabled(this.getWorld())) {
            BlockPos pos = cir.getReturnValue();
            BlockPos above = pos.up();
            if (
                    !this.getWorld().getBlockState(pos).isIn(BlockTags.RAILS) &&
                    this.getWorld().getBlockState(above).isIn(BlockTags.RAILS)
            ) {
                cir.setReturnValue(above);
            }
        }
    }
}
