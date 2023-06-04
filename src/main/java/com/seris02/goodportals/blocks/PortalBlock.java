package com.seris02.goodportals.blocks;

import java.util.Optional;

import com.seris02.goodportals.ClientHandler;
import com.seris02.goodportals.GoodPortals;
import com.seris02.goodportals.connection.RefreshCamoModel;
import com.seris02.goodportals.storage.DataStorage;
import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.network.PacketDistributor;

public class PortalBlock extends BaseEntityBlock {

	public PortalBlock(Block.Properties properties) {
		super(properties);
	}
	
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PortalBlockEntity(pos, state);
	}

	public Optional<BlockState> getCamoState(BlockAndTintGetter level, BlockPos pos) {
		PortalBlockEntity en = (PortalBlockEntity) level.getBlockEntity(pos);
		if (en != null) {
			if (en.camoState != null) {
				return Optional.of(en.camoState);
			}
		}
		return Optional.empty();
	}
	
	public void attack(BlockState state, Level level, BlockPos pos, Player p) {
		PortalBlockEntity en = (PortalBlockEntity) level.getBlockEntity(pos);
		if (en != null && en.controllerPos != null) {
			if (!PortalStorage.get(level).doesPlayerHaveAccessToBoth(pos, level.dimension(), p)) {
				return;
			}
		}
		super.attack(state, level, pos, p);
	}
	
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (player.getItemInHand(hand).getItem() instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			if (block instanceof PortalBlock || block instanceof PortalController || !Block.isShapeFullBlock(block.defaultBlockState().getVisualShape(level, pos, CollisionContext.empty()))) {
				return super.use(state, level, pos, player, hand, hit);
			}
			PortalBlockEntity en = (PortalBlockEntity) level.getBlockEntity(pos);
			if (en != null) {
				if (en.controllerPos != null) {
					DataStorage e = PortalStorage.get(level).getDataWithPos(en.controllerPos, level.dimension());
					if (e != null && !e.canPlayerAccess(player)) {
						return super.use(state, level, pos, player, hand, hit);
					}
				}
				en.camoState = blockItem.getBlock().defaultBlockState();
				if (!level.isClientSide) {
					GoodPortals.channel.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), new RefreshCamoModel(pos, player.getItemInHand(hand)));
				} else {
					ClientHandler.putCamoBeRenderer(en, blockItem.getBlock().defaultBlockState());
					if (en.camoState.getLightEmission(level, pos) > 0) {
						level.getChunkSource().getLightEngine().checkBlock(pos);
					}
				}
				return InteractionResult.SUCCESS;
			}
		}
		return super.use(state, level, pos, player, hand, hit);
	}
	
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor worldAccess, BlockPos blockPos, BlockPos neighborPos) {
		if (!worldAccess.isClientSide()) {
			if (worldAccess instanceof Level) {
				Level world = (Level) worldAccess;
				PortalBlockEntity en = (PortalBlockEntity) world.getBlockEntity(blockPos);
				if (en != null) {
					en.notifyPortal();
				}
			}
		}
		return super.updateShape(state, direction, neighborState, worldAccess, blockPos, neighborPos);
	}
	
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
		if (level.getBlockEntity(pos) instanceof PortalBlockEntity en && en.controllerPos != null) {
			if (!PortalStorage.get(level).doesPlayerHaveAccessToBoth(pos, level.dimension(), player)) {
				return false;
			}
		}
		return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
	}
	
	public void destroy(LevelAccessor worldAccess, BlockPos pos, BlockState state) {
		if (!worldAccess.isClientSide()) {
			if (worldAccess instanceof Level) {
				Level world = (Level) worldAccess;
				PortalBlockEntity en = (PortalBlockEntity) world.getBlockEntity(pos);
				if (en != null) {
					en.notifyPortal();
				}
			}
		}
		super.destroy(worldAccess, pos, state);
	}
}
