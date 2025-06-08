package io.github.sequentialentropy.mixin;

import com.mojang.datafixers.util.Pair;
import io.github.sequentialentropy.RailShapeHelper;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.entity.vehicle.MinecartController;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashSet;
import java.util.List;

@Mixin(ExperimentalMinecartController.class)
public abstract class ExperimentalMinecartControllerMixin extends MinecartController {
	protected ExperimentalMinecartControllerMixin(AbstractMinecartEntity minecart) {
		super(minecart);
	}

	/**
	 * @author SequentialEntropy
	 * @reason Custom physics for Smooth Minecarts
	 */
	@Overwrite
	public void moveOnRail(ServerWorld world) {
		ExperimentalMinecartController thisObject = (ExperimentalMinecartController) (Object) this;
		ExperimentalMinecartControllerInvoker thisInvoker = (ExperimentalMinecartControllerInvoker) thisObject;
		AbstractMinecartEntityInvoker minecartInvoker = (AbstractMinecartEntityInvoker) this.minecart;

		int LOOKAHEAD_DISTANCE = 30;
		// Get the current velocity of the minecart
		Vec3d currentVelocity = this.getVelocity();
		// Get the position of the rail or the cart itself
		BlockPos blockPos = this.minecart.getRailOrMinecartPos();
		// Get the block state at that position
		BlockState blockState = this.getWorld().getBlockState(blockPos);
		// Check whether the current block is a rail
		boolean isOnRail = AbstractRailBlock.isRail(blockState);

		// Sync the minecart's "on rail" status
		if (this.minecart.isOnRail() != isOnRail) {
			this.minecart.setOnRail(isOnRail);
			// Adjust the cart’s position to match the rail orientation
			thisObject.adjustToRail(blockPos, blockState, false);
		}

		if (isOnRail) {
			// Trigger landing logic (e.g., for fall damage or sounds)
			this.minecart.onLanding();
			// Reset internal position state (likely used for interpolation)
			this.minecart.resetPosition();

			// Special handling for activator rails (which may trigger events)
			if (blockState.isOf(Blocks.ACTIVATOR_RAIL)) {
				this.minecart.onActivatorRail(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockState.get(PoweredRailBlock.POWERED));
			}

			// Determine rail shape (e.g., straight, curved, sloped)
			RailShape railShape = blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty());

			// Calculate new velocity along the rail
			Vec3d adjustedVelocity = this.calcNewHorizontalVelocity(world, currentVelocity.getHorizontal(), blockPos, blockState, railShape);

			// Apply new velocity
			this.setVelocity(adjustedVelocity);

			// If the velocity is very small, stop the cart
			if (adjustedVelocity.length() < 1.0E-5F) {
				this.setVelocity(Vec3d.ZERO);
			} else {
				// Choose facing direction
				Pair<Vec3i, Vec3i> railDirections = AbstractMinecartEntity.getAdjacentRailPositionsByShape(railShape);
				boolean b = (adjustedVelocity.dotProduct(new Vec3d(railDirections.getFirst())) < adjustedVelocity.dotProduct(new Vec3d(railDirections.getSecond())));
				Vec3i ahead = b ? railDirections.getSecond() : railDirections.getFirst();

				// Get rails ahead
				List<Vec3d> points = RailShapeHelper.lookAhead(world, blockPos, ahead, LOOKAHEAD_DISTANCE);

				Vec3d newVelocity = derailmentAdjustedVelocity(adjustedVelocity, blockPos, points);

				// Apply new velocity
				this.setVelocity(newVelocity);
				this.setPos(this.getPos().add(newVelocity));
			}
		} else {
			// If not on a rail, handle off-rail movement
			minecartInvoker.invokeMoveOffRail(world);
		}

		// Determine the position delta for orientation and interpolation
		Vec3d currentPosition = this.getPos();
		Vec3d deltaPosition = currentPosition.subtract(this.minecart.getLastRenderPos());
		double deltaLength = deltaPosition.length();

		if (deltaLength > 1.0E-5F) {
			if (!(deltaPosition.horizontalLengthSquared() > 1.0E-5F)) {
				if (!this.minecart.isOnRail()) {
					float adjustedPitch = this.minecart.isOnGround() ? 0.0F : MathHelper.lerpAngleDegrees(0.2F, this.getPitch(), 0.0F);
					this.setPitch(adjustedPitch);
				}
			} else {
				// Set yaw and pitch based on direction of movement
				float yaw = 180.0F - (float)(Math.atan2(deltaPosition.z, deltaPosition.x) * 180.0 / Math.PI);
				float pitch = this.minecart.isOnGround() && !this.minecart.isOnRail()
						? 0.0F
						: 90.0F - (float)(Math.atan2(deltaPosition.horizontalLength(), deltaPosition.y) * 180.0 / Math.PI);

				if (this.minecart.isYawFlipped()) {
					yaw += 180.0F;
					pitch *= -1.0F;
				}

				thisInvoker.invokeSetAngles(yaw, pitch);
			}

			// Add to interpolation steps for rendering (likely client-side)
			thisObject.stagingLerpSteps.add(new ExperimentalMinecartController.Step(
					currentPosition,
					this.getVelocity(),
					this.getYaw(),
					this.getPitch(),
					(float)Math.min(deltaLength, this.getMaxSpeed(world))
			));
		} else if (currentVelocity.horizontalLengthSquared() > 0.0) {
			// If not moving significantly but still has horizontal speed
			thisObject.stagingLerpSteps.add(new ExperimentalMinecartController.Step(
					currentPosition,
					this.getVelocity(),
					this.getYaw(),
					this.getPitch(),
					1.0F));
		}

		// If moved or in the first iteration, handle collision checks
		this.minecart.tickBlockCollision(); // Called twice for redundancy or separate pass types
		this.minecart.tickBlockCollision();
	}

	@Unique
	private Vec3d calcNewHorizontalVelocity(
			ServerWorld world, Vec3d horizontalVelocity, BlockPos pos, BlockState railState, RailShape railShape
	) {

		ExperimentalMinecartController thisObject = (ExperimentalMinecartController) (Object) this;
		ExperimentalMinecartControllerInvoker thisInvoker = (ExperimentalMinecartControllerInvoker) thisObject;
		AbstractMinecartEntityInvoker minecartInvoker = (AbstractMinecartEntityInvoker) this.minecart;

		// --- Apply slope adjustment once ---
		// Adjust velocity if the rail is on a slope (e.g., ascending/descending)
		Vec3d updatedVelocity = horizontalVelocity;
		Vec3d slopeAdjustedVelocity = thisInvoker.invokeApplySlopeVelocity(horizontalVelocity, railShape);
		if (slopeAdjustedVelocity.horizontalLengthSquared() != horizontalVelocity.horizontalLengthSquared()) {
			updatedVelocity = slopeAdjustedVelocity;
		}

		// --- Apply player input on the first iteration only ---
		Vec3d initialVelocity = thisInvoker.invokeApplyInitialVelocity(updatedVelocity);
		if (initialVelocity.horizontalLengthSquared() != updatedVelocity.horizontalLengthSquared()) {
			updatedVelocity = initialVelocity;
		}

		// --- Apply passive deceleration from unpowered rail if not already done ---
		Vec3d deceleratedVelocity = thisInvoker.invokeDecelerateFromPoweredRail(updatedVelocity, railState);
		if (deceleratedVelocity.horizontalLengthSquared() != updatedVelocity.horizontalLengthSquared()) {
			updatedVelocity = deceleratedVelocity;
		}

		// --- Apply general slowdown (friction) on first iteration ---
		updatedVelocity = minecartInvoker.invokeApplySlowdown(updatedVelocity);
		if (updatedVelocity.lengthSquared() > 0.0) {
			// Clamp velocity to the cart's maximum allowed speed
			double clampedSpeed = Math.min(updatedVelocity.length(), minecartInvoker.invokeGetMaxSpeed(world));
			updatedVelocity = updatedVelocity.normalize().multiply(clampedSpeed);
		}

		// --- Apply powered rail acceleration if not already done ---
		Vec3d acceleratedVelocity = thisInvoker.invokeAccelerateFromPoweredRail(updatedVelocity, pos, railState);
		if (acceleratedVelocity.horizontalLengthSquared() != updatedVelocity.horizontalLengthSquared()) {
			updatedVelocity = acceleratedVelocity;
		}

		// Final velocity after all adjustments
		return updatedVelocity;
	}

	@Unique
	private Vec3d derailmentAdjustedVelocity(Vec3d adjustedVelocity, BlockPos blockPos, List<Vec3d> points) {
		final int CHECK_FOR_RAILS_AHEAD = 15;

		// Store rails ahead in set
		HashSet<Vec3i> pointsSet = new HashSet<>();
		pointsSet.add(new Vec3i(blockPos.getX(), 0, blockPos.getZ()));
		for (Vec3d point : points) {
			Vec3i point3i = BlockPos.ofFloored(point);
			pointsSet.add(new Vec3i(point3i.getX(), 0, point3i.getZ()));
		}

		// Get speed and current position
		Vec3d currentPos = this.getPos();
		double currentSpeed = adjustedVelocity.horizontalLength();

		// Stores the velocity of the first successful raycast attempt for that length
		Vec3d[] raycastCache = new Vec3d[Math.max(CHECK_FOR_RAILS_AHEAD, 1)];

		// Derailment prevention - repeatedly retry to smoothen the curve with fewer samples until the predicted position lands on the predicted path
		for (int i = points.size(); i > 0; i--) {
			Vec3d averageDirection = RailShapeHelper.averageDirection(currentPos, points.subList(0, i));
			Vec3d averageVelocity = averageDirection.multiply(currentSpeed);

			BlockPos predictedBlock = BlockPos.ofFloored(currentPos.add(averageVelocity));

			Vec3i key = new Vec3i(predictedBlock.getX(), 0, predictedBlock.getZ());
			if (!pointsSet.contains(key)) continue;
			// Cache the unscaled velocity
			if (raycastCache[0] == null) raycastCache[0] = averageVelocity;

			// Repeatedly scale the velocity (raycast) and check if it is a valid rail
			for (int distance = 1; distance <= CHECK_FOR_RAILS_AHEAD; distance++) {
				Vec3d scaledVelocity = averageDirection.multiply(distance);
				BlockPos raycastBlock = BlockPos.ofFloored(currentPos.add(scaledVelocity));

				// If block isn't a rail along the path, end raycast
				key = new Vec3i(raycastBlock.getX(), 0, raycastBlock.getZ());
				if (!pointsSet.contains(key)) break;

				if (distance == CHECK_FOR_RAILS_AHEAD) return averageVelocity;

				if (raycastCache[distance] == null) raycastCache[distance] = averageVelocity;
			}
		}

		// Return the velocity for the longest raycast attempt
		for (int distance = raycastCache.length - 1; distance >= 0; distance--) {
			Vec3d velocity = raycastCache[distance];
			if (velocity != null) {
				return velocity;
			}
		}

		return adjustedVelocity;
	}
}