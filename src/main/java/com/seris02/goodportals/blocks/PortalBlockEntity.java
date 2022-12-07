package com.seris02.goodportals.blocks;

import com.seris02.goodportals.ClientHandler;
import com.seris02.goodportals.LinkedPortal;
import com.seris02.goodportals.datagen.PortalContent;
import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;

public class PortalBlockEntity extends BlockEntity {
	
	public BlockState camoState;
	public BlockPos controllerPos;

	public PortalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
	
	public PortalBlockEntity(BlockPos pos, BlockState state) {
		this(PortalContent.PORTAL_ENTITY.get(), pos, state);
	}
	
	@Override
	public IModelData getModelData() {
		return new ModelDataMap.Builder().withInitial(PortalBakedModel.CAMO_STATE, Blocks.AIR.defaultBlockState()).build();
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
	}
	
	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		this.camoState = NbtUtils.readBlockState(nbt.getCompound("camo"));
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
			PortalControllerEntity a = (PortalControllerEntity) level.getBlockEntity(controllerPos);
			if (a != null) {
				LinkedPortal p = a.getPortal();
				if (p != null) {
					p.notifyPlaceholderUpdate();
				}
			}
			PortalStorage.get(level).checkFrame(controllerPos, level.dimension());
		}
	}
	
	public void removePortal() {
		if (controllerPos != null && !level.isClientSide) {
			PortalControllerEntity a = (PortalControllerEntity) level.getBlockEntity(controllerPos);
			if (a != null) {
				LinkedPortal p = a.getPortal();
				if (p != null) {
					p.markShouldBreak();
				}
			}
		}
	}
	

}
