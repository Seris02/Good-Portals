package com.seris02.goodportals.blocks;

import java.util.List;
import java.util.UUID;

import com.seris02.goodportals.LinkedPortal;
import com.seris02.goodportals.PortalUtils;
import com.seris02.goodportals.datagen.PortalContent;
import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;

public class PortalControllerEntity extends PortalBlockEntity {
	public LinkedPortal portal;
	public UUID portalUUID = Util.NIL_UUID;
	public BlockPortalShape shape;
	
	public PortalControllerEntity(BlockPos pos, BlockState state) {
		super(PortalContent.PORTAL_CONTROLLER_ENTITY.get(), pos, state);
	}
	
	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		if (this.portalUUID == null) portalUUID = Util.NIL_UUID;
		nbt.putUUID("u", portalUUID);
		nbt.putBoolean("s", shape == null);
		if (this.shape != null) {
			nbt.put("shape", PortalUtils.getCheapTagFromShape(shape));
		}
	}
	
	public void setPortal(LinkedPortal p) {
		portal = p;
		portalUUID = p.getUUID();
	}
	
	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		if (!nbt.getBoolean("s")) {
			shape = PortalUtils.getShapeFromCheapTag(nbt.getCompound("shape"));
		}
		if (nbt.hasUUID("u")) {
			portalUUID = nbt.getUUID("u");
		}
	}
	
	public LinkedPortal getPortal() {
		if (portal != null) {
			if (portal.isRemoved() || portal.shouldBreak) {
				portal = null;
				portalUUID = null;
				return null;
			}
			return portal;
		}
		refreshPortal();
		return portal;
	}
	
	public void refreshPortal() {
		if (portalUUID == null) return;
		Level l = this.level;
		Entity pe = ((IEWorld) l).portal_getEntityLookup().get(portalUUID);
		if (pe instanceof LinkedPortal) {
			portal = (LinkedPortal) pe;
		}
	}
	
	@Override
	public void removePortal(boolean keepData) {
		portalUUID = null;
		if (!keepData) {
			setNoControllerForAll();
		}
		getPortal();
		if (portal != null) {
			portal.makeWay();
		}
	}
	
	public void setControllerForAll(BlockPortalShape shape) {
		shape.frameAreaWithoutCorner.forEach(blockPos -> {
			if (level.getBlockEntity(blockPos) instanceof PortalBlockEntity p) {
				p.controllerPos = this.getBlockPos();
				p.axis = shape.axis;
			}
		});
		this.shape = new BlockPortalShape(shape.toTag());
	}
	
	public void setNoControllerForAll() {
		if (shape == null) return;
		shape.frameAreaWithoutCorner.forEach(blockPos -> {
			if (level.getBlockEntity(blockPos) instanceof PortalBlockEntity p) {
				p.controllerPos = null;
				p.axis = null;
			}
		});
		shape = null;
	}
	
}
