package com.seris02.goodportals.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.seris02.goodportals.PortalUtils;
import com.seris02.goodportals.blocks.PortalControllerEntity;
import com.seris02.goodportals.LinkedPortal;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.dimension.DimId;

public class DataStorage {
	public enum Var {
		NAME,
		SHAPE,
		DIMENSION,
		IN_USE,
		ID,
		SPECIFIC_PLAYER,
		ONLY_TELEPORT_SELF,
		RENDER_PLAYER,
		IS_LIGHT_SOURCE,
		ALL
	}
	public BlockPortalShape shape;
	public BlockPos controllerPos;
	public String name;
	public ResourceKey<Level> dimension;
	public String inUse;
	public String ID;
	public List<String> matching;
	public String specific_player;
	public boolean onlyTeleportSelf;
	public boolean renderPlayers;
	public boolean isLightSource;
	public Direction dir; //serverside only, never needs updating on the client
	public UUID portalUUID;
	public LinkedPortal portal = null;
	
	public DataStorage(BlockPortalShape shape, BlockPos pos, ResourceKey<Level> dimension, String name) {
		this.shape = shape;
		this.controllerPos = pos;
		this.name = name;
		this.dimension = dimension;
		this.inUse = "";
		this.specific_player = "";
		this.onlyTeleportSelf = true;
		this.renderPlayers = true;
		this.isLightSource = true;
		this.portalUUID = Util.NIL_UUID;
		ID = name.substring(0, Math.min(name.length(), 2));
		Random a = new Random();
		for (int x = 0; x < 4; x++) {
			ID += String.valueOf("abcdefghijklmnopqrstuvwxyz".charAt(a.nextInt(26)));
		}
		matching = new ArrayList<String>();
	}
	
	public CompoundTag save(boolean forClient) {
		CompoundTag tag = new CompoundTag();
		tag.putString("n", name);
		tag.put("p", NbtUtils.writeBlockPos(controllerPos));
		DimId.putWorldId(tag, "d", dimension);
		tag.putString("id", ID);
		tag.putString("iu", inUse);
		tag.putBoolean("o", onlyTeleportSelf);
		tag.putBoolean("r", renderPlayers);
		tag.putBoolean("i", isLightSource);
		tag.putString("s", specific_player);
		ListTag x = new ListTag();
		for (String s : matching) {
			x.add(StringTag.valueOf(s));
		}
		tag.put("m", x);
		tag.putBoolean("c", forClient);
		if (!forClient) {
			if (portalUUID == null) {
				portalUUID = Util.NIL_UUID;
			}
			tag.putUUID("u", portalUUID);
			if (dir == null) {
				dir = Direction.DOWN;
			}
			tag.putString("di", dir.getName());
			tag.put("sh", PortalUtils.getCheapTagFromShape(shape));
		}
		return tag;
	}
	
	public DataStorage(CompoundTag tag) {
		this.controllerPos = NbtUtils.readBlockPos(tag.getCompound("p"));
		this.name = tag.getString("n");
		this.dimension = DimId.getWorldId(tag, "d", PortalUtils.isRunningOnClient());
		this.ID = tag.getString("id");
		this.inUse = tag.getString("iu");
		this.onlyTeleportSelf = tag.getBoolean("o");
		this.renderPlayers = tag.getBoolean("r");
		this.isLightSource = tag.getBoolean("i");
		this.specific_player = tag.getString("s");
		this.matching = new ArrayList<String>();
		ListTag x = tag.getList("m", Tag.TAG_STRING);
		for (int c = 0; c < x.size(); c++) {
			matching.add(x.getString(c));
		}
		if (!tag.getBoolean("c")) {
			this.shape = PortalUtils.getShapeFromCheapTag(tag.getCompound("sh"));
			this.dir = Direction.byName(tag.getString("di"));
			this.portalUUID = tag.getUUID("u");
			if (this.portalUUID == null) portalUUID = Util.NIL_UUID;
		}
		refreshPortal();
	}
	
	public Level getDimensionLevel() {
		return PortalUtils.getLevelFromDimensionKey(dimension);
	}
	
	public void addMatch(DataStorage d) {
		matching.add(d.ID);
		d.matching.add(ID);
	}
	
	public void removeMatch(String ID) {
		matching.remove(ID);
	}
	
	public PortalControllerEntity getControllerEntity() {
		if (getDimensionLevel().getBlockEntity(controllerPos) instanceof PortalControllerEntity pe) return pe;
		return null;
	}

	public boolean canPlayerAccess(Player player) {
		return player == null || specific_player == null || specific_player.isBlank() || (specific_player.equals(player.getScoreboardName()));
	}
	
	public boolean canPlayerAccessNoNull(Player player) {
		return player != null && (specific_player == null || specific_player.isBlank() || (specific_player.equals(player.getScoreboardName())));
	}
	
	public String toString() {
		return this.save(false).toString();
	}
	
	public void refreshPortal() {
		if (PortalUtils.isRunningOnClient()) return;
		Level l = PortalUtils.getServerLevelFromDimensionKey(dimension);
		Entity pe = ((IEWorld) l).portal_getEntityLookup().get(portalUUID);
		if (pe instanceof LinkedPortal) {
			portal = (LinkedPortal) pe;
		}
	}
	
	public LinkedPortal getPortal() {
		if (portal == null) refreshPortal();
		return portal;
	}
}