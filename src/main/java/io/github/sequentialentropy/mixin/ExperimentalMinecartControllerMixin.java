package io.github.sequentialentropy.mixin;

import com.mojang.datafixers.util.Pair;
import io.github.sequentialentropy.RailShapeHelper;
import io.github.sequentialentropy.config.Rules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartBehavior;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

@Mixin(NewMinecartBehavior.class)
public abstract class NewMinecartBehaviorMixin extends MinecartBehavior {
	protected NewMinecartBehaviorMixin(AbstractMinecart minecart) {
		super(minecart);
	}

	/**
	 * @author SequentialEntropy
	 * @reason Custom physics for Smooth Minecarts
	 */
	@Overwrite
	public void moveAlongTrack(ServerLevel world) {
		NewMinecartBehavior thisObject = (NewMinecartBehavior) (Object) this;
		ExperimentalMinecartControllerInvoker thisInvoker = (ExperimentalMinecartControllerInvoker) thisObject;
		AbstractMinecartEntityInvoker minecartInvoker = (AbstractMinecartEntityInvoker) this.minecart;

        // Get gamerule
		int SAMPLING_DISTANCE = world.getGameRules().getInt(Rules.SAMPLING_DISTANCE);

		// Get the current velocity of the minecart
		Vec3 currentVelocity = this.getDeltaMovement();
		// Get the position of the rail or the cart itself
		BlockPos blockPos = this.minecart.getCurrentBlockPosOrRailBelow();
		// Get the block state at that position
		BlockState blockState = this.level().getBlockState(blockPos);
		// Check whether the current block is a rail
		boolean isOnRail = BaseRailBlock.isRail(blockState);

		// Sync the minecart's "on rail" status
		if (this.minecart.isOnRails() != isOnRail) {
			this.minecart.setOnRails(isOnRail);
			// Adjust the cart’s position to match the rail orientation
			thisObject.adjustToRails(blockPos, blockState, false);
		}

		if (isOnRail) {
			// Trigger landing logic (e.g., for fall damage or sounds)
			this.minecart.resetFallDistance();
			// Reset internal position state (likely used for interpolation)
			this.minecart.setOldPosAndRot();

			// Special handling for activator rails (which may trigger events)
			if (blockState.is(Blocks.ACTIVATOR_RAIL)) {
				this.minecart.activateMinecart(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockState.getValue(PoweredRailBlock.POWERED));
			}

			// Determine rail shape (e.g., straight, curved, sloped)
			RailShape railShape = blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty());

			// Calculate new velocity along the rail
			Vec3 adjustedVelocity = this.calculateTrackSpeed(world, currentVelocity.horizontal(), blockPos, blockState, railShape);

			// Apply new velocity
			this.setDeltaMovement(adjustedVelocity);

			// If the velocity is very small, stop the cart
			if (adjustedVelocity.length() < 1.0E-5F) {
				this.setDeltaMovement(Vec3.ZERO);
			} else {
				// Choose facing direction
				Pair<Vec3i, Vec3i> railDirections = AbstractMinecart.exits(railShape);
				boolean b = (adjustedVelocity.dot(new Vec3(railDirections.getFirst())) < adjustedVelocity.dot(new Vec3(railDirections.getSecond())));
				Vec3i ahead = b ? railDirections.getSecond() : railDirections.getFirst();

				// Get rails ahead
				List<Vec3> points = RailShapeHelper.lookAhead(world, blockPos, ahead, SAMPLING_DISTANCE);

				Vec3 newVelocity = derailmentAdjustedVelocity(world, adjustedVelocity, blockPos, points);

				// Apply new velocity
				this.setDeltaMovement(newVelocity);
				this.setPos(this.position().add(newVelocity));
			}
		} else {
			// If not on a rail, handle off-rail movement
			minecartInvoker.invokeComeOffTrack(world);
		}

		// Determine the position delta for orientation and interpolation
		Vec3 currentPosition = this.position();
		Vec3 deltaPosition = currentPosition.subtract(this.minecart.oldPosition());
		double deltaLength = deltaPosition.length();

		if (deltaLength > 1.0E-5F) {
			if (!(deltaPosition.horizontalDistanceSqr() > 1.0E-5F)) {
				if (!this.minecart.isOnRails()) {
					float adjustedPitch = this.minecart.onGround() ? 0.0F : Mth.rotLerp(0.2F, this.getXRot(), 0.0F);
					this.setXRot(adjustedPitch);
				}
			} else {
				// Set yaw and pitch based on direction of movement
				float yaw = 180.0F - (float)(Math.atan2(deltaPosition.z, deltaPosition.x) * 180.0 / Math.PI);
				float pitch = this.minecart.onGround() && !this.minecart.isOnRails()
						? 0.0F
						: 90.0F - (float)(Math.atan2(deltaPosition.horizontalDistance(), deltaPosition.y) * 180.0 / Math.PI);

				if (this.minecart.isFlipped()) {
					yaw += 180.0F;
					pitch *= -1.0F;
				}

				thisInvoker.invokeSetRotation(yaw, pitch);
			}

			// Add to interpolation steps for rendering (likely client-side)
			thisObject.lerpSteps.add(new NewMinecartBehavior.MinecartStep(
					currentPosition,
					this.getDeltaMovement(),
					this.getYRot(),
					this.getXRot(),
					(float)Math.min(deltaLength, this.getMaxSpeed(world))
			));
		} else if (currentVelocity.horizontalDistanceSqr() > 0.0) {
			// If not moving significantly but still has horizontal speed
			thisObject.lerpSteps.add(new NewMinecartBehavior.MinecartStep(
					currentPosition,
					this.getDeltaMovement(),
					this.getYRot(),
					this.getXRot(),
					1.0F));
		}

		// If moved or in the first iteration, handle collision checks
		this.minecart.applyEffectsFromBlocks(); // Called twice for redundancy or separate pass types
		this.minecart.applyEffectsFromBlocks();
	}

	@Unique
	private Vec3 calculateTrackSpeed(
			ServerLevel world, Vec3 horizontalVelocity, BlockPos pos, BlockState railState, RailShape railShape
	) {

		NewMinecartBehavior thisObject = (NewMinecartBehavior) (Object) this;
		ExperimentalMinecartControllerInvoker thisInvoker = (ExperimentalMinecartControllerInvoker) thisObject;
		AbstractMinecartEntityInvoker minecartInvoker = (AbstractMinecartEntityInvoker) this.minecart;

		// --- Apply slope adjustment once ---
		// Adjust velocity if the rail is on a slope (e.g., ascending/descending)
		Vec3 updatedVelocity = horizontalVelocity;
		Vec3 slopeAdjustedVelocity = thisInvoker.invokeCalculateSlopeSpeed(horizontalVelocity, railShape);
		if (slopeAdjustedVelocity.horizontalDistanceSqr() != horizontalVelocity.horizontalDistanceSqr()) {
			updatedVelocity = slopeAdjustedVelocity;
		}

		// --- Apply player input on the first iteration only ---
		Vec3 playerInputVelocity = thisInvoker.invokeCalculatePlayerInputSpeed(updatedVelocity);
		if (playerInputVelocity.horizontalDistanceSqr() != updatedVelocity.horizontalDistanceSqr()) {
			updatedVelocity = playerInputVelocity;
		}

		// --- Apply passive deceleration from unpowered rail if not already done ---
		Vec3 deceleratedVelocity = thisInvoker.invokeCalculateHaltTrackSpeed(updatedVelocity, railState);
		if (deceleratedVelocity.horizontalDistanceSqr() != updatedVelocity.horizontalDistanceSqr()) {
			updatedVelocity = deceleratedVelocity;
		}

		// --- Apply general slowdown (friction) on first iteration ---
		updatedVelocity = minecartInvoker.invokeApplyNaturalSlowdown(updatedVelocity);
		if (updatedVelocity.lengthSqr() > 0.0) {
			// Clamp velocity to the cart's maximum allowed speed
			double clampedSpeed = Math.min(updatedVelocity.length(), minecartInvoker.invokeGetMaxSpeed(world));
			updatedVelocity = updatedVelocity.normalize().scale(clampedSpeed);
		}

		// --- Apply powered rail acceleration if not already done ---
		Vec3 acceleratedVelocity = thisInvoker.invokeCalculateBoostTrackSpeed(updatedVelocity, pos, railState);
		if (acceleratedVelocity.horizontalDistanceSqr() != updatedVelocity.horizontalDistanceSqr()) {
			updatedVelocity = acceleratedVelocity;
		}

		// Final velocity after all adjustments
		return updatedVelocity;
	}

	@Unique
	private Vec3 derailmentAdjustedVelocity(ServerLevel world, Vec3 adjustedVelocity, BlockPos blockPos, List<Vec3> points) {
        // Get gamerule
		final int STRAIGHTNESS_PRECHECK_DISTANCE = world.getGameRules().getInt(Rules.STRAIGHTNESS_PRECHECK_DISTANCE);

		// Store rails ahead in set
		HashSet<Vec3i> pointsSet = new HashSet<>();
		pointsSet.add(new Vec3i(blockPos.getX(), 0, blockPos.getZ()));
		for (Vec3 point : points) {
			Vec3i point3i = BlockPos.containing(point);
			pointsSet.add(new Vec3i(point3i.getX(), 0, point3i.getZ()));
		}

		// Get speed and current position
		Vec3 currentPos = this.position();
		double currentSpeed = adjustedVelocity.horizontalDistance();

		// Stores the velocity of the first successful raycast attempt for that length
		Vec3[] raycastCache = new Vec3[Math.max(STRAIGHTNESS_PRECHECK_DISTANCE, 1)];

		// Derailment prevention - repeatedly retry to smoothen the curve with fewer samples until the predicted position lands on the predicted path
		for (int i = points.size(); i > 0; i--) {
			Vec3 averageDirection = RailShapeHelper.averageDirection(currentPos, points.subList(0, i));
			Vec3 averageVelocity = averageDirection.scale(currentSpeed);

			BlockPos predictedBlock = BlockPos.containing(currentPos.add(averageVelocity));

			Vec3i key = new Vec3i(predictedBlock.getX(), 0, predictedBlock.getZ());
			if (!pointsSet.contains(key)) continue;
			// Cache the unscaled velocity
			if (raycastCache[0] == null) raycastCache[0] = averageVelocity;

			// Repeatedly scale the velocity (raycast) and check if it is a valid rail
			for (int distance = 1; distance <= STRAIGHTNESS_PRECHECK_DISTANCE; distance++) {
				Vec3 scaledVelocity = averageDirection.scale(distance);
				BlockPos raycastBlock = BlockPos.containing(currentPos.add(scaledVelocity));

				// If block isn't a rail along the path, end raycast
				key = new Vec3i(raycastBlock.getX(), 0, raycastBlock.getZ());
				if (!pointsSet.contains(key)) break;

				if (distance == STRAIGHTNESS_PRECHECK_DISTANCE) return averageVelocity;

				if (raycastCache[distance] == null) raycastCache[distance] = averageVelocity;
			}
		}

		// Return the velocity for the longest raycast attempt
		for (int distance = raycastCache.length - 1; distance >= 0; distance--) {
			Vec3 velocity = raycastCache[distance];
			if (velocity != null) {
				return velocity;
			}
		}

		return adjustedVelocity;
	}
}