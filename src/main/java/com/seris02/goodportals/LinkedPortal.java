package com.seris02.goodportals;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.seris02.goodportals.datagen.PortalContent;
import com.seris02.goodportals.storage.DataStorage;
import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.imm_ptl.core.platform_specific.IPRegistry;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.PortalAnimation;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.portal.nether_portal.GeneralBreakablePortal;
import qouteall.imm_ptl.core.render.PortalGroup;
import qouteall.q_misc_util.my_util.BoxPredicate;

public class LinkedPortal extends GeneralBreakablePortal {

	public static EntityType<LinkedPortal> entityType = PortalContent.LINKED_PORTAL.get();
	public boolean original = false;
	public BlockPos controllerPos;
	public long then = 0;
	public boolean shouldBreak = false;
	public String specificPlayer = null;
	public boolean onlyTeleportSelf = true;
	public boolean isLightSource = true;
	public boolean alreadyInformed = false;
	
	public LinkedPortal(EntityType<?> entityType, Level world) {
		super(entityType, world);
		this.doRenderPlayer = false;
		this.hasCrossPortalCollision = false;
		this.controllerPos = null;
		//this.animation = new PortalAnimation(Curve.sine, 0, false);
	}
	
	public void readAdditionalSaveData(CompoundTag tag) {
		original = tag.getBoolean("o");
		controllerPos = NbtUtils.readBlockPos(tag.getCompound("cpos"));
		specificPlayer = tag.getString("sp");
		onlyTeleportSelf = tag.getBoolean("ts");
		super.readAdditionalSaveData(tag);
	}
	
	public void addAdditionalSaveData(CompoundTag tag) {
		tag.putBoolean("o", original);
		if (controllerPos != null) {
			tag.put("cpos", NbtUtils.writeBlockPos(controllerPos));
		}
		if (specificPlayer != null) {
			tag.putString("sp", specificPlayer);
		}
		tag.putBoolean("ts", onlyTeleportSelf);
		super.addAdditionalSaveData(tag);
	}
	
	@Override
	protected boolean isPortalIntactOnThisSide() {
		return teleportable && isControllerThere() && blockPortalShape.frameAreaWithoutCorner.stream()
				.allMatch(blockPos -> {
					Block x = level.getBlockState(blockPos).getBlock();
					if (x == PortalContent.PORTAL_BLOCK.get() || x == PortalContent.PORTAL_CONTROLLER.get()) {
						return true;
					}
					return false;
				});
	}
	
	public boolean playerHasAccess(String player) {
		if (specificPlayer == null && original) {
			DataStorage e = PortalStorage.get().getDataWithPos(controllerPos, this.getOriginDim());
			if (e != null && e.specific_player != null) {
				specificPlayer = e.specific_player;
				onlyTeleportSelf = e.onlyTeleportSelf;
				if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
					a.specificPlayer = e.specific_player;
					a.onlyTeleportSelf = e.onlyTeleportSelf;
				}
			}
		}
		return specificPlayer == null || specificPlayer.isBlank() || !onlyTeleportSelf || (specificPlayer.equals(player));
	}
	
	
	public boolean isInFrontOfPortal(Vec3 pos) {
		if (teleportable == false || (PortalUtils.isRunningOnClient() && !playerHasAccess(Minecraft.getInstance().player.getScoreboardName()))) {
			return false;
		}
		return super.isInFrontOfPortal(pos);
	}
	
	public boolean isPointInPortalProjection(Vec3 pos) {
		if (teleportable == false || (PortalUtils.isRunningOnClient() && !playerHasAccess(Minecraft.getInstance().player.getScoreboardName()))) {
			return false;
		}
		return super.isPointInPortalProjection(pos);
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
	
	public void notifyPlaceholderUpdate() {
		super.notifyPlaceholderUpdate();
	}
	
	public void setToPlayer(String player) {
		specificPlayer = player;
		if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			a.specificPlayer = player;
		}
		if (!PortalUtils.isRunningOnClient()) PortalCommand.reloadPortal(this);
	}

	public void setToTeleportSelf(boolean teleport) {
		onlyTeleportSelf = teleport;
		if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			a.onlyTeleportSelf = teleport;
		}
		if (!PortalUtils.isRunningOnClient()) PortalCommand.reloadPortal(this);
	}
	
	public void createOrDestroyPlaceholders() {
		if (this.level.isClientSide) return;
		if (isLightSource) {
			blockPortalShape.area.forEach((pos) -> {
				if (this.level.getBlockState(pos).getBlock() == Blocks.AIR || this.level.getBlockState(pos).getBlock() == IPRegistry.NETHER_PORTAL_BLOCK.get()) {
					this.level.setBlockAndUpdate(pos, IPRegistry.NETHER_PORTAL_BLOCK.get().defaultBlockState().setValue(PortalPlaceholderBlock.AXIS, blockPortalShape.axis));
				}
			});
		} else {
			removePlaceholders();
		}
	}
	
	public void removePlaceholders() {
		blockPortalShape.area.forEach((pos) -> {
			if (this.level.getBlockState(pos).getBlock() == IPRegistry.NETHER_PORTAL_BLOCK.get()) {
				this.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
		});
	}

	public void setLightSource(boolean source) {
		isLightSource = source;
		createOrDestroyPlaceholders();
		if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			a.isLightSource = source;
		}
	}
	
	public void setdoRenderPlayer(boolean render) {
		this.doRenderPlayer = render;
		if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			a.doRenderPlayer = render;
		}
		if (!PortalUtils.isRunningOnClient()) PortalCommand.reloadPortal(this);
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
		/*if (PortalManipulation.findFlippedPortal(this) instanceof LinkedPortal a) {
			Quaternion sa = a.getRotationD().toMcQuaternion();
			sa.mul(x);
			a.rotation = sa;
			PortalCommand.reloadPortal(a);
		}*/
		PortalCommand.reloadPortal(this);
	}
	
	public void remove(RemovalReason reason) {
		super.remove(reason);
		if (reason == RemovalReason.KILLED && original) {
			if (alreadyInformed == false) PortalStorage.get().removePortal(this);
			if (this.blockPortalShape != null) removePlaceholders();
		}
	}
	
	public void makeWay() {
		makeWay(true);
	}
	
	private void makeWay(boolean mark) {
		teleportable = false;
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
		remove(RemovalReason.KILLED);
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
	
	public boolean isOneWay() {
		return reversePortalId == null || reversePortalId.equals(Util.NIL_UUID);
	}
}
