package com.waylays.chromeneon.common.block;

import com.mojang.serialization.MapCodec;
import com.waylays.chromeneon.common.entity.SeatEntity;
import com.waylays.chromeneon.common.network.OpenRipperdocPayload;
import com.waylays.chromeneon.common.registry.ModEntities;
import com.waylays.chromeneon.common.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Map;

/**
 * The ripperdoc chair: the only place where cyberware is installed or removed. Using it seats the player (they
 * ride an invisible seat) and opens the surgery interface; the server then accepts surgery requests only from a
 * player seated in a chair. Sneaking gets up.
 */
public class RipperdocChairBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<RipperdocChairBlock> CODEC = simpleCodec(RipperdocChairBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = buildShapes();
    /** Height of the seat surface above the block's bottom. */
    public static final double SEAT_Y = 0.42;

    public RipperdocChairBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        VoxelShape base = Shapes.or(Block.box(3, 0, 3, 13, 3, 13), Block.box(1, 5, 1, 15, 9, 15));
        shapes.put(Direction.NORTH, Shapes.or(base, Block.box(2, 9, 12, 14, 16, 15)));
        shapes.put(Direction.SOUTH, Shapes.or(base, Block.box(2, 9, 1, 14, 16, 4)));
        shapes.put(Direction.WEST, Shapes.or(base, Block.box(12, 9, 2, 15, 16, 14)));
        shapes.put(Direction.EAST, Shapes.or(base, Block.box(1, 9, 2, 4, 16, 14)));
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

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RipperdocChairBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        BlockPos seatedAt = seatedIn(sp);
        if (!pos.equals(seatedAt)) {
            if (occupied(level, pos)) return InteractionResult.CONSUME;
            if (player.isPassenger()) player.stopRiding();
            SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), level);
            seat.setPos(pos.getX() + 0.5, pos.getY() + SEAT_Y - 0.2, pos.getZ() + 0.5);
            seat.setYRot(state.getValue(FACING).toYRot());
            level.addFreshEntity(seat);
            sp.startRiding(seat, true);
            sp.setYRot(state.getValue(FACING).toYRot());
            level.playSound(null, pos, ModSounds.CHAIR_SIT.get(), SoundSource.BLOCKS, 0.8F, 1F);
        }
        if (sp.connection != null && sp.connection.hasChannel(OpenRipperdocPayload.TYPE)) {
            PacketDistributor.sendToPlayer(sp, new OpenRipperdocPayload(pos));
        }
        return InteractionResult.CONSUME;
    }

    /** Whether someone already sits in the chair at {@code pos}. */
    public static boolean occupied(Level level, BlockPos pos) {
        return !level.getEntitiesOfClass(SeatEntity.class, new AABB(pos), s -> !s.getPassengers().isEmpty()).isEmpty();
    }

    /** The chair the player sits in, or null: surgery is only possible there. */
    public static BlockPos seatedIn(ServerPlayer player) {
        if (!(player.getVehicle() instanceof SeatEntity seat)) return null;
        BlockPos pos = seat.blockPosition();
        return player.level().getBlockState(pos).getBlock() instanceof RipperdocChairBlock ? pos : null;
    }
}
