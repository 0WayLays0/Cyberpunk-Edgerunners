package com.waylays.chromeneon.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * The ripperdoc's chair, phase 1: placed facing the player, with a seat-and-backrest shape. It is deliberately
 * inert for now; sitting, the block entity and the install interface arrive with the cyberware system (phase 3).
 */
public class RipperdocChairBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RipperdocChairBlock> CODEC = simpleCodec(RipperdocChairBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

    public RipperdocChairBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        VoxelShape base = Block.box(2, 0, 2, 14, 9, 14);
        // The backrest sits on the side opposite to the direction the chair faces
        shapes.put(Direction.NORTH, Shapes.or(base, Block.box(2, 9, 12, 14, 16, 14)));
        shapes.put(Direction.SOUTH, Shapes.or(base, Block.box(2, 9, 2, 14, 16, 4)));
        shapes.put(Direction.WEST, Shapes.or(base, Block.box(12, 9, 2, 14, 16, 14)));
        shapes.put(Direction.EAST, Shapes.or(base, Block.box(2, 9, 2, 4, 16, 14)));
        return shapes;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
}
