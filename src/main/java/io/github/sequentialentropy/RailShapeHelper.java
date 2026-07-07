package io.github.sequentialentropy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public class RailShapeHelper {
    public static List<Vec3> lookAhead(ServerLevel world, BlockPos blockPos, Vec3i dir, int distance) {
        ArrayList<Vec3> positions = new ArrayList<>(distance);
        Vec3i headed = dir;
        Vec3 currentPos = Vec3.atBottomCenterOf(blockPos).add(0, 0.1, 0);

        for (int i = 0; i < distance; i++) {
            BlockState currentState = world.getBlockState(BlockPos.containing(currentPos));
            if (!BaseRailBlock.isRail(currentState)) {
                Vec3 posBelow = currentPos.subtract(0, 1, 0);
                BlockState stateBelow = world.getBlockState(BlockPos.containing(posBelow));
                if (!BaseRailBlock.isRail(stateBelow)) {
                    break;
                }
                currentPos = posBelow;
                currentState = stateBelow;
            }
            RailShape currentShape = currentState.getValue(((BaseRailBlock)currentState.getBlock()).getShapeProperty());
            headed = next(new Vec3i(headed.getX(), 0, headed.getZ()), currentShape);
            currentPos = currentPos.add(new Vec3(headed));
            positions.add(currentPos);
        }
        return positions;
    }

    private static Vec3i next(Vec3i headed, RailShape nextShape) {
        if (headed.equals(Direction.NORTH.getUnitVec3i())) {
            switch (nextShape) {
                case NORTH_SOUTH:
                case NORTH_EAST:
                case NORTH_WEST:
                case ASCENDING_SOUTH:
                    return Direction.NORTH.getUnitVec3i();
                case SOUTH_EAST:
                    return Direction.EAST.getUnitVec3i();
                case SOUTH_WEST:
                    return Direction.WEST.getUnitVec3i();
                case ASCENDING_NORTH:
                    return Direction.NORTH.getUnitVec3i().above();
            }
        } else if (headed.equals(Direction.SOUTH.getUnitVec3i())) {
            switch (nextShape) {
                case NORTH_SOUTH:
                case SOUTH_EAST:
                case SOUTH_WEST:
                case ASCENDING_NORTH:
                    return Direction.SOUTH.getUnitVec3i();
                case NORTH_EAST:
                    return Direction.EAST.getUnitVec3i();
                case NORTH_WEST:
                    return Direction.WEST.getUnitVec3i();
                case ASCENDING_SOUTH:
                    return Direction.SOUTH.getUnitVec3i().above();
            }
        } else if (headed.equals(Direction.EAST.getUnitVec3i())) {
            switch (nextShape) {
                case EAST_WEST:
                case NORTH_EAST:
                case SOUTH_EAST:
                case ASCENDING_WEST:
                    return Direction.EAST.getUnitVec3i();
                case NORTH_WEST:
                    return Direction.NORTH.getUnitVec3i();
                case SOUTH_WEST:
                    return Direction.SOUTH.getUnitVec3i();
                case ASCENDING_EAST:
                    return Direction.EAST.getUnitVec3i().above();
            }
        } else if (headed.equals(Direction.WEST.getUnitVec3i())) {
            switch (nextShape) {
                case EAST_WEST:
                case NORTH_WEST:
                case SOUTH_WEST:
                case ASCENDING_EAST:
                    return Direction.WEST.getUnitVec3i();
                case NORTH_EAST:
                    return Direction.NORTH.getUnitVec3i();
                case SOUTH_EAST:
                    return Direction.SOUTH.getUnitVec3i();
                case ASCENDING_WEST:
                    return Direction.WEST.getUnitVec3i().above();
            }
        }
        return Vec3i.ZERO;
    }

    public static Vec3 averageDirection(Vec3 currentPos, List<Vec3> points) {
        double totalX = 0;
        double totalY = 0;
        double totalZ = 0;
        double totalWeight = 0;

        for (Vec3 point : points) {
            totalX += (point.x - currentPos.x);
            totalY += (point.y - currentPos.y);
            totalZ += (point.z - currentPos.z);
            totalWeight += 1;
        }

        totalX /= totalWeight;
        totalY /= totalWeight;
        totalZ /= totalWeight;

        return new Vec3(totalX, totalY, totalZ).normalize();
    }
}
