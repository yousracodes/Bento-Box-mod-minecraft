package com.bentobox;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;

public final class BentoBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final int compartments;
    public BentoBlock(int compartments) {
        super(Properties.of().strength(0.3F).noOcclusion().sound(SoundType.BAMBOO_WOOD));
        this.compartments = compartments;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    public int compartments() { return compartments; }
    // The imported Classic is wider in model units; normalize each size separately.
    public static float renderScale(int compartments) { return compartments == 9 ? 0.87F : 0.58F; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        float scale = renderScale(compartments);
        double width = (compartments == 9 ? 16.5 : 22.1) * scale;
        double depth = (compartments == 9 ? 15.825 : 16.865) * scale;
        if (state.getValue(FACING).getAxis() == Direction.Axis.X) {
            double swap = width;
            width = depth;
            depth = swap;
        }
        return box(8-width/2, 0, 8-depth/2, 8+width/2, 6.45*scale, 8+depth/2);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BentoBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity player, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof BentoBlockEntity bento) bento.setBox(stack);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BentoBlockEntity bento) {
            if (player.isShiftKeyDown()) bento.toggleLid();
            else if (player instanceof ServerPlayer serverPlayer) NetworkHooks.openScreen(serverPlayer, bento, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeVarInt(compartments);
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player.mayBuild()) level.removeBlock(pos, false);
    }
    // A single contents-bearing item drops for every removal path, including creative pickup.
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) { return List.of(); }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof BentoBlockEntity bento) popResource(level, pos, bento.boxStack());
            super.onRemove(state, level, pos, next, moving);
        }
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof BentoBlockEntity bento ? bento.boxStack() : new ItemStack(asItem());
    }
}
