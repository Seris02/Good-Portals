package com.seris02.goodportals;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.seris02.goodportals.datagen.PortalContent;
import com.seris02.goodportals.storage.DataStorage;
import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.portal.nether_portal.GeneralBreakablePortal;
import qouteall.imm_ptl.core.render.PortalGroup;

public class LinkedPortal extends GeneralBreakablePortal {
	
	public static EntityType<LinkedPortal> entityType = PortalContent.LINKED_PORTAL.get();
	public boolean original = false;
	public BlockPos controllerPos;
	public long then = 0;
	public boolean shouldBreak = false;
	public String specificPlayer = null;
	public boolean onlyTeleportSelf = false;
	
	public LinkedPortal(EntityType<?> entityType, Level world) {
		super(entityType, world);
		this.doRenderPlayer = false;
		this.hasCrossPortalCollision = false;
		this.controllerPos = null;
	}
	
	@Override
	protected boolean isPortalIntactOnThisSide() {
		return isControllerThere() && blockPortalShape.frameAreaWithoutCorner.stream()
				.allMatch(blockPos -> {
					Block x = level.getBlockState(blockPos).getBlock();
					if (x == PortalContent.PORTAL_BLOCK.get() || x == PortalContent.PORTAL_CONTROLLER.get()) {
						return true;
					}
					return false;
				});
	}
	
	public boolean playerHasAccess(String player) {
		if (specificPlayer == null) {
			DataStorage e = PortalStorage.get().getDataWithPos(controllerPos, this.getOriginDim());
			if (e != null && e.specific_player != null) {
				specificPlayer = e.specific_player;
				onlyTeleportSelf = e.onlyTeleportSelf;
			}
		}
		return specificPlayer == null || specificPlayer.isBlank() || (onlyTeleportSelf && specificPlayer.equals(player));
	}
	
	public boolean canTeleportEntity(Entity e) {
		if (e instanceof Player p) {
			if (!playerHasAccess(p.getScoreboardName())) {
				return false;
			}
		}
		return super.canTeleportEntity(e);
	}
	
	public boolean isControllerThere() {
		return !original || (controllerPos != null && level.getBlockState(controllerPos).getBlock() == PortalContent.PORTAL_CONTROLLER.get());
	}
	
	public void setToPlayer(String player) {
		specificPlayer = player;
		if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			a.specificPlayer = player;
		}
	}
	
	public void setToTeleportSelf(boolean teleport) {
		onlyTeleportSelf = teleport;
		if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			a.onlyTeleportSelf = teleport;
		}
	}
	
	public void setdoRenderPlayer(boolean render) {
		this.doRenderPlayer = render;
		if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			a.doRenderPlayer = render;
			PortalCommand.reloadPortal(a);
		}
		PortalCommand.reloadPortal(this);
	}
	
	public void flipPortalAxis(boolean way) {
		if (System.currentTimeMillis() - then < 100) return;
		then = System.currentTimeMillis();
		Vector3f d = null;
		int wayA = way ? 0 : 1;
		int wayB = way ? 1 : 0;
		switch(blockPortalShape.axis) {
			case X:
				d = new Vector3f(wayA,wayB,0);
				break;
			case Y:
				d = new Vector3f(wayB,wayA,0);
				break;
			case Z:
				d = new Vector3f(0,wayB,wayA);
				break;
		}
		if (d == null) return;
		Quaternion x = new Quaternion(d, (float) 180, true);
		Quaternion s = getRotationD().toMcQuaternion();
		s.mul(x);
		rotation = s;
		if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			Quaternion sa = a.getRotationD().toMcQuaternion();
			sa.mul(x);
			a.rotation = sa;
			PortalCommand.reloadPortal(a);
		}
		PortalCommand.reloadPortal(this);
	}
	
	public void readAdditionalSaveData(CompoundTag tag) {
		original = tag.getBoolean("original");
		controllerPos = NbtUtils.readBlockPos(tag.getCompound("cpos"));
		specificPlayer = tag.getString("splayer");
		super.readAdditionalSaveData(tag);
	}
	
	public void addAdditionalSaveData(CompoundTag tag) {
		tag.putBoolean("original", original);
		if (controllerPos != null) {
			tag.put("cpos", NbtUtils.writeBlockPos(controllerPos));
		}
		if (specificPlayer != null) {
			tag.putString("splayer", specificPlayer);
		}
		super.addAdditionalSaveData(tag);
	}
	
	private void breakPortalOnThisSide() {
		if (original) {
			PortalStorage.get().removePortal(this);
		}
		this.remove(RemovalReason.KILLED);
	}
	
	public void remove(RemovalReason reason) {
		super.remove(reason);
		if (reason == RemovalReason.KILLED && original) {
			PortalStorage.get().removePortal(this);
		}
	}
	
	public void makeWay() {
		makeWay(true);
	}
	
	private void makeWay(boolean mark) {
		teleportable = false;
		setInvisible(true);
		setOriginPos(new Vec3(0,0,0));
		markToBreak(true);
		if (mark) {
			if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
				a.makeWay(false);
			}
			if (PortalManipulation.findReversePortal(this) instanceof LinkedPortal a) {
				a.makeWay(false);
			}
			if (PortalManipulation.findParallelPortal(this) instanceof LinkedPortal a) {
				a.makeWay(false);
			}
		}
	}
	
	public void markShouldBreak() {
		super.markShouldBreak();
		if (shouldBreak == false) {
			shouldBreak = true;
			markToBreak(false);
		}
	}
	
	public void markToBreak(boolean into) {
		if (into) {
			shouldBreak = true;
			markShouldBreak();
			return;
		}
		if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			a.markToBreak(true);
		}
		if (PortalManipulation.findReversePortal(this) instanceof LinkedPortal a) {
			a.markToBreak(true);
		}
		if (PortalManipulation.findParallelPortal(this) instanceof LinkedPortal a) {
			a.markToBreak(true);
		}
	}
	/*
	public void notifyPlaceholderUpdate() {
		super.notifyPlaceholderUpdate();
		if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			a.notifyPlaceholderUpdate();
		}
		if (PortalManipulation.findReversePortal(this) instanceof LinkedPortal a) {
			a.notifyPlaceholderUpdate();
		}
		if (PortalManipulation.findParallelPortal(this) instanceof LinkedPortal a) {
			a.notifyPlaceholderUpdate();
		}
	}*/
	
	public boolean isOneWay() {
		return reversePortalId == null || reversePortalId.equals(Util.NIL_UUID);
	}
}
