package com.seris02.goodportals.blocks;

import java.util.List;

import com.seris02.goodportals.LinkedPortal;
import com.seris02.goodportals.datagen.PortalContent;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;

public class PortalControllerEntity extends PortalBlockEntity {
	public LinkedPortal portal;
	public Vec3 portalPosition;
	public Vec3 portalNormal;
	public BlockPortalShape shape;
	
	public PortalControllerEntity(BlockPos pos, BlockState state) {
		super(PortalContent.PORTAL_CONTROLLER_ENTITY.get(), pos, state);
	}
	
	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		if (this.portalPosition != null) {
			nbt.putDouble("pos1", portalPosition.x());
			nbt.putDouble("pos2", portalPosition.y());
			nbt.putDouble("pos3", portalPosition.z());
		}
		if (this.portalNormal != null) {
			nbt.putDouble("no1", portalNormal.x());
			nbt.putDouble("no2", portalNormal.y());
			nbt.putDouble("no3", portalNormal.z());
		}
	}
	
	public void setPortal(LinkedPortal p) {
		portal = p;
		portalPosition = p.position();
		portalNormal = p.getNormal();
	}
	
	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		portalPosition = new Vec3(nbt.getDouble("pos1"),nbt.getDouble("pos2"),nbt.getDouble("pos3"));
		portalNormal = new Vec3(nbt.getDouble("no1"),nbt.getDouble("no2"),nbt.getDouble("no3"));
	}
	
	public LinkedPortal getPortal() {
		if (portal != null) {
			if (portal.isRemoved() || portal.shouldBreak) {
				portal = null;
				portalPosition = null;
				portalNormal = null;
				setNoControllerForAll();
				return null;
			}
			return portal;
		}
		refreshPortal();
		return portal;
	}
	
	public void refreshPortal() {
		if (portalPosition == null || portalNormal == null) return;
		if (!this.level.isClientSide) {
			ServerLevel world = (ServerLevel) this.level;
			List<Portal> e = PortalManipulation.getPortalCluster(world, portalPosition, portalNormal, portal -> (portal instanceof LinkedPortal));
			for (Portal n : e) {
				if (n instanceof LinkedPortal a) {
					if (a.original) {
						portal = a;
					}
				}
			}
		}
	}
	
	@Override
	public void removePortal() {
		portalPosition = null;
		portalNormal = null;
		setNoControllerForAll();
		getPortal();
		if (portal != null) {
			portal.markShouldBreak();
		}
	}
	
	public void setControllerForAll(BlockPortalShape shape) {
		shape.frameAreaWithoutCorner.forEach(blockPos -> {
			if (level.getBlockEntity(blockPos) instanceof PortalBlockEntity p) {
				p.controllerPos = this.getBlockPos();
			}
		});
		this.shape = shape;
	}
	
	public void setNoControllerForAll() {
		if (shape == null) return;
		shape.frameAreaWithoutCorner.forEach(blockPos -> {
			if (level.getBlockEntity(blockPos) instanceof PortalBlockEntity p) {
				p.controllerPos = null;
			}
		});
	}
	
}
