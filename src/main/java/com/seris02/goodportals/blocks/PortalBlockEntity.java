package com.seris02.goodportals.blocks;

import com.seris02.goodportals.ClientHandler;
import com.seris02.goodportals.LinkedPortal;
import com.seris02.goodportals.datagen.PortalContent;
import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class PortalBlockEntity extends BlockEntity {
	
	public BlockState camoState;
	public BlockPos controllerPos;
	public Direction.Axis axis;

	public PortalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
	
	public PortalBlockEntity(BlockPos pos, BlockState state) {
		this(PortalContent.PORTAL_ENTITY.get(), pos, state);
	}
	
	@Override
	public ModelData getModelData() {
		return ModelData.builder().with(PortalBakedModel.CAMO_STATE, Blocks.AIR.defaultBlockState()).build();
	}
	
	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		if (camoState != null) {
			nbt.put("camo", NbtUtils.writeBlockState(camoState));
		}
		nbt.putBoolean("a", controllerPos == null);
		if (controllerPos != null) {
			nbt.put("cpos", NbtUtils.writeBlockPos(controllerPos));
		}
		nbt.putInt("e", axis == Direction.Axis.X  ? 1 : (axis == Direction.Axis.Y ? 2 : 3));
	}
	
	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		this.camoState = NbtUtils.readBlockState(nbt.getCompound("camo"));
		int a = nbt.getInt("e");
		axis = a == 1  ? Direction.Axis.X : (a == 2 ? Direction.Axis.Y : Direction.Axis.Z);
		if (!nbt.getBoolean("a")) {
			this.controllerPos = NbtUtils.readBlockPos(nbt.getCompound("cpos"));
		}
	}
	
	@Override
	public CompoundTag getUpdateTag() {
		return saveWithoutMetadata();
	}
	
	@Override
	public void handleUpdateTag(CompoundTag tag) {
		if (tag == null) return;
		load(tag);

		if (level != null && level.isClientSide) {

			if (camoState != null)
				ClientHandler.putCamoBeRenderer(this, camoState);
			else
				ClientHandler.CAMO_RENDER_DELEGATE.removeDelegateOf(this);
		}
	}
	
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
		handleUpdateTag(packet.getTag());
	}
	
	public void notifyPortal() {
		if (controllerPos != null && !level.isClientSide) {
			if (level.getBlockEntity(controllerPos) instanceof PortalControllerEntity a) {
				LinkedPortal p = a.getPortal();
				if (p != null) {
					p.notifyPlaceholderUpdate();
				}
			}
			PortalStorage.get(level).checkFrameFromPortalBlock(controllerPos, level.dimension());
		}
	}
	
	public void removePortal(boolean keepData) {
		if (controllerPos != null && !level.isClientSide) {
			if (level.getBlockEntity(controllerPos) instanceof PortalControllerEntity a) {
				a.removePortal(true);
			}
		}
	}
	

}
