package io.github.sequentialentropy;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class RailShapeHelper {
    public static List<Vec3d> lookAhead(ServerWorld world, BlockPos blockPos, Vec3i dir, int distance) {
        ArrayList<Vec3d> positions = new ArrayList<>(distance);
        Vec3i headed = dir;
        Vec3d currentPos = Vec3d.ofBottomCenter(blockPos).add(0, 0.1, 0);

        for (int i = 0; i < distance; i++) {
            BlockState currentState = world.getBlockState(BlockPos.ofFloored(currentPos));
            if (!AbstractRailBlock.isRail(currentState)) {
                Vec3d posBelow = currentPos.subtract(0, 1, 0);
                BlockState stateBelow = world.getBlockState(BlockPos.ofFloored(posBelow));
                if (!AbstractRailBlock.isRail(stateBelow)) {
                    break;
                }
                currentPos = posBelow;
                currentState = stateBelow;
            }
            RailShape currentShape = currentState.get(((AbstractRailBlock)currentState.getBlock()).getShapeProperty());
            headed = next(new Vec3i(headed.getX(), 0, headed.getZ()), currentShape);
            currentPos = currentPos.add(new Vec3d(headed));
            positions.add(currentPos);
        }
        return positions;
    }

    private static Vec3i next(Vec3i headed, RailShape nextShape) {
        if (headed.equals(Direction.NORTH.getVector())) {
            switch (nextShape) {
                case NORTH_SOUTH:
                case NORTH_EAST:
                case NORTH_WEST:
                case ASCENDING_SOUTH:
                    return Direction.NORTH.getVector();
                case SOUTH_EAST:
                    return Direction.EAST.getVector();
                case SOUTH_WEST:
                    return Direction.WEST.getVector();
                case ASCENDING_NORTH:
                    return Direction.NORTH.getVector().up();
            }
        } else if (headed.equals(Direction.SOUTH.getVector())) {
            switch (nextShape) {
                case NORTH_SOUTH:
                case SOUTH_EAST:
                case SOUTH_WEST:
                case ASCENDING_NORTH:
                    return Direction.SOUTH.getVector();
                case NORTH_EAST:
                    return Direction.EAST.getVector();
                case NORTH_WEST:
                    return Direction.WEST.getVector();
                case ASCENDING_SOUTH:
                    return Direction.SOUTH.getVector().up();
            }
        } else if (headed.equals(Direction.EAST.getVector())) {
            switch (nextShape) {
                case EAST_WEST:
                case NORTH_EAST:
                case SOUTH_EAST:
                case ASCENDING_WEST:
                    return Direction.EAST.getVector();
                case NORTH_WEST:
                    return Direction.NORTH.getVector();
                case SOUTH_WEST:
                    return Direction.SOUTH.getVector();
                case ASCENDING_EAST:
                    return Direction.EAST.getVector().up();
            }
        } else if (headed.equals(Direction.WEST.getVector())) {
            switch (nextShape) {
                case EAST_WEST:
                case NORTH_WEST:
                case SOUTH_WEST:
                case ASCENDING_EAST:
                    return Direction.WEST.getVector();
                case NORTH_EAST:
                    return Direction.NORTH.getVector();
                case SOUTH_EAST:
                    return Direction.SOUTH.getVector();
                case ASCENDING_WEST:
                    return Direction.WEST.getVector().up();
            }
        }
        return Vec3i.ZERO;
    }

    public static Vec3d averageDirection(Vec3d currentPos, List<Vec3d> points) {
        double totalX = 0;
        double totalY = 0;
        double totalZ = 0;
        double totalWeight = 0;

        for (Vec3d point : points) {
            totalX += (point.x - currentPos.x);
            totalY += (point.y - currentPos.y);
            totalZ += (point.z - currentPos.z);
            totalWeight += 1;
        }

        totalX /= totalWeight;
        totalY /= totalWeight;
        totalZ /= totalWeight;

        return new Vec3d(totalX, totalY, totalZ).normalize();
    }
}
