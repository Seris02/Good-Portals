package com.seris02.goodportals.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.seris02.goodportals.LinkedPortal;
import com.seris02.goodportals.PortalUtils;
import com.seris02.goodportals.blocks.PortalControllerEntity;
import com.seris02.goodportals.connection.PortalStorageRefresh;
import com.seris02.goodportals.connection.SingleDataStorageRefresh;
import com.seris02.goodportals.datagen.PortalContent;
import com.seris02.goodportals.storage.DataStorage.Var;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.imm_ptl.core.portal.custom_portal_gen.form.DiligentMatcher;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.dimension.DimId;

public class PortalStorage extends SavedData {
	
	private static PortalStorage clientSide = new PortalStorage();
	
	private List<DataStorage> data;
	private List<StorageEmittee> emittees;
	
	public PortalStorage() {
		data = new ArrayList<DataStorage>();
		emittees = new ArrayList<StorageEmittee>();
	}
	
	///this isn't really necessary, using the level is only for clientside check
	public static PortalStorage get(Level level) {
		boolean x = level == null ? PortalUtils.isRunningOnClient() : level.isClientSide;
		if (x) {
			if (clientSide == null) {
				clientSide = new PortalStorage();
			}
			return clientSide;
		}
		ServerLevel overworld = (ServerLevel) PortalUtils.getServerLevelFromDimensionKey(Level.OVERWORLD);
		return overworld.getDataStorage().computeIfAbsent((nbt) -> {
			PortalStorage p = new PortalStorage();
			p.fromNBT(nbt);
			return p;
		}, () -> new PortalStorage(), "portal_storage");
	}
	
	public static PortalStorage get() {
		return PortalStorage.get(null);
	}
	public void debug() {
		for (DataStorage d : data) {
			System.out.println(d.name);
			System.out.println(d.shape.axis.name());
			System.out.println(PortalUtils.getCheapTagFromShape(d.shape).toString());
		}
	}
	@Override
	public CompoundTag save(CompoundTag tag) {
		return save(tag, false);
	}
	
	public CompoundTag save(CompoundTag tag, boolean forClient) {
		ListTag t = new ListTag();
		for (DataStorage d : data) {
			t.add(d.save(forClient));
		}
		tag.put("data", t);
		return tag;
	}
	
	public void fromNBT(CompoundTag tag) {
		data = new ArrayList<DataStorage>();
		ListTag t = tag.getList("data", Tag.TAG_COMPOUND);
		for (int x = 0; x < t.size(); x++) {
			DataStorage d = new DataStorage((CompoundTag) t.get(x));
			data.add(d);
		}
		sendFullUpdate();
	}
	
	public void syncToPlayers() {
		if (PortalUtils.isRunningOnClient()) {return;}
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(player -> syncToPlayer(player));
	}
	
	public void syncToPlayer(ServerPlayer player) {
		if (PortalUtils.isRunningOnClient()) {return;}
		PortalStorageRefresh psr = new PortalStorageRefresh(save(new CompoundTag(), true));
		PortalUtils.sendToPlayer(psr, player);
	}
	
	public void syncSpecificVarToPlayers(Var type, DataStorage storage, LinkedPortal specific) {
		if (PortalUtils.isRunningOnClient()) {return;}
		SingleDataStorageRefresh s = new SingleDataStorageRefresh(storage.ID, type, storage, specific);
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(player -> PortalUtils.sendToPlayer(s, player));
	}
	
	public boolean doesPlayerHaveAccessToBoth(String ID, Player player) {
		DataStorage e = this.getDataWithID(ID);
		if (e == null) return true;
		DataStorage r = this.getDataWithID(e.inUse);
		if ((e != null && !e.canPlayerAccess(player)) || (r != null && !r.canPlayerAccess(player))) {
			return false;
		}
		return true;
	}
	
	public boolean doesPlayerHaveAccessToBoth(BlockPos pos, ResourceKey<Level> dimension, Player player) {
		DataStorage e = this.getDataWithPos(pos, dimension);
		if (e == null) return true;
		DataStorage r = this.getDataWithID(e.inUse);
		if ((e != null && !e.canPlayerAccess(player)) || (r != null && !r.canPlayerAccess(player))) {
			return false;
		}
		return true;
	}
	
	public void renamePortal(BlockPos controllerPos, ResourceKey<Level> dimension, String name, ServerPlayer player) {
		if (PortalUtils.isRunningOnClient()) {return;}
		DataStorage x = this.getDataWithPos(controllerPos, dimension);
		if (x == null || !x.canPlayerAccess(player)) {return;}
		x.name = name;
		syncSpecificVarToPlayers(Var.NAME, x, null);
		setDirty(true);
	}
	
	public void removePortal(LinkedPortal portal) {
		removePortal(null, null, portal, false);
	}
	
	public void removePortal(BlockPos controllerPos, ResourceKey<Level> dimension, boolean removeData) {
		removePortal(controllerPos, dimension, null, removeData);
	}
	
	public void removePortal(String ID, boolean removeData) {
		removePortal(ID, removeData, null);
	}
	
	public void removePortal(String ID, boolean removeData, ServerPlayer player) {
		DataStorage d = this.getDataWithID(ID);
		if (d != null && d.canPlayerAccess(player)) {
			DataStorage e = this.getDataWithID(d.inUse);
			if (e != null && !e.canPlayerAccess(player)) {
				return;
			}
			removePortal(d.controllerPos, d.dimension, null, removeData);
		}
	}
	
	private void removePortal(BlockPos controllerPos, ResourceKey<Level> dimension, LinkedPortal portal, boolean removeData) {
		if (PortalUtils.isRunningOnClient()) {return;}
		DataStorage d = null;
		if (portal != null) {
			for (DataStorage da : data) {
				da.refreshPortal();
				if (da.portal == portal) {
					d = da;
				}
			}
			controllerPos = portal.controllerPos;
			dimension = portal.getOriginDim();
		}
		if (controllerPos == null || dimension == null) {
			return;
		}
		d = d == null ? this.getDataWithPos(controllerPos, dimension) : d;
		if (d == null) return;
		ServerLevel level = PortalUtils.getServerLevelFromDimensionKey(dimension);
		boolean remove = removeData ||  (PortalUtils.findPortalShape(level, controllerPos, d.dir) == null);
		if (d.portal == null) d.refreshPortal();
		if (d.portal != null) {
			d.portal.alreadyInformed = true;
			d.portal.makeWay();
		} else if (portal != null && !portal.isRemoved()) {
			portal.alreadyInformed = true;
			portal.makeWay();
		}
		if (remove) {
			if (level.getBlockEntity(controllerPos) instanceof PortalControllerEntity pe) {
				pe.removePortal(false);
			}
			removeData(d.ID);
			syncToPlayers();
		} else {
			DataStorage da = getDataWithID(d.inUse);
			if (da != null) {
				da.inUse = "";
				syncSpecificVarToPlayers(DataStorage.Var.IN_USE, da, null);
			}
			d.inUse = "";
			syncSpecificVarToPlayers(DataStorage.Var.IN_USE, d, null);
		}
		setDirty(true);
	}
	
	private void removeData(String ID) {
		DataStorage e = this.getDataWithID(ID);
		for (DataStorage f : data) {
			f.removeMatch(ID);
		}
		data.remove(e);
		setDirty(true);
	}
	
	public void setToPlayer(String ID, ServerPlayer player, boolean on) {
		this.setBooleanVarAndUpdate(ID, player, Var.SPECIFIC_PLAYER, true, (e) -> {
			e.specific_player = e.specific_player.isBlank() ? player.getScoreboardName() : "";
		}, (d, p) -> p.setToPlayer(d.specific_player));
	}
	
	public void setRenderSelf(String ID, ServerPlayer player, boolean renderPlayers) {
		this.setBooleanVarAndUpdate(ID, player, Var.RENDER_PLAYER, false, (e) -> e.renderPlayers = renderPlayers, (d, p) -> p.setdoRenderPlayer(renderPlayers));
	}
	
	public void setOnlyTeleportSelf(String ID, ServerPlayer player, boolean teleportSelf) {
		this.setBooleanVarAndUpdate(ID, player, Var.ONLY_TELEPORT_SELF, false, (e) -> e.onlyTeleportSelf = teleportSelf, (d, p) -> p.setToTeleportSelf(teleportSelf));
	}
	
	public void setIsLightSource(String ID, ServerPlayer player, boolean isLightSource) {
		this.setBooleanVarAndUpdate(ID, player, Var.IS_LIGHT_SOURCE, true, (e) -> e.isLightSource = isLightSource, (d, p) -> p.setLightSource(isLightSource));
	}
	
	public void setBooleanVarAndUpdate(String ID, ServerPlayer player, Var type, boolean checkOtherPortalAccess, Consumer<DataStorage> d, BiConsumer<DataStorage, LinkedPortal> p) {
		if (PortalUtils.isRunningOnClient()) {return;}
		DataStorage e = this.getDataWithID(ID);
		if (e == null) return;
		if (!e.canPlayerAccess(player)) {
			return; //no.
		}
		if (checkOtherPortalAccess) {
			DataStorage r = this.getDataWithID(e.inUse);
			if (r != null && !r.canPlayerAccess(player)) {
				return;
			}
		}
		LinkedPortal pa = null;
		d.accept(e);
		if (!e.inUse.isBlank()) {
			pa = e.getPortal();
			p.accept(e, pa);
		}
		this.syncSpecificVarToPlayers(type, e, pa);
		setDirty(true);
	}
	
	public boolean addFrame(BlockPos controllerPos, ResourceKey<Level> dimension, String name) {
		return this.addFrame(controllerPos, dimension, name, PortalUtils.getPredicates(true), PortalUtils.getPredicates(false));
	}
	public boolean addFrame(BlockPos controllerPos, ResourceKey<Level> dimension, String name, Predicate<BlockState> air, Predicate<BlockState> frame) {
		if (PortalUtils.isRunningOnClient()) {return false;}
		if (this.getDataWithPos(controllerPos, dimension) != null) {return false;}
		ServerLevel level = PortalUtils.getServerLevelFromDimensionKey(dimension);
		if (level.getBlockState(controllerPos).getBlock() != PortalContent.PORTAL_CONTROLLER.get()) {return false;}
		DataStorage d = PortalUtils.findDataStorage(level, controllerPos, name, data);
		if (d == null) {return false;}
		for (DataStorage toMatch : getMatchingPortalFrames(d.shape, dimension)) {
			d.addMatch(toMatch);
		}
		if (level.getBlockEntity(controllerPos) instanceof PortalControllerEntity pe) {
			pe.setControllerForAll(d.shape);
		}
		data.add(d);
		syncToPlayers();
		setDirty(true);
		return true;
	}
	
	public List<DataStorage> getMatchingPortalFrames(BlockPortalShape shape, ResourceKey<Level> dimension) {
		return getMatchingPortalFrames(shape, dimension, PortalUtils.getPredicates(true), PortalUtils.getPredicates(false));
	}
	
	public List<DataStorage> getMatchingPortalFrames(BlockPortalShape shape, ResourceKey<Level> dimension, Predicate<BlockState> air, Predicate<BlockState> frame) {
		if (PortalUtils.isRunningOnClient()) {return null;}
		List<DataStorage> list = new ArrayList<DataStorage>();
		List<DataStorage> inUse = new ArrayList<DataStorage>();
		List<DiligentMatcher.TransformedShape> matchable = DiligentMatcher.getMatchableShapeVariants(shape, 20);
		BlockPos.MutableBlockPos temp2 = new BlockPos.MutableBlockPos();
		ServerLevel world = PortalUtils.getServerLevelFromDimensionKey(dimension);
		boolean w = true;
		k:
			if (w) {
				for (int c = 0; c < data.size(); c++) {
					DataStorage d = data.get(c);
					if (!this.checkFrame(d.ID)) {
						continue;
					}
					if (d.shape.area.size() != shape.area.size()) {
						continue;
					}
					ServerLevel world2 = PortalUtils.getServerLevelFromDimensionKey(d.dimension);
					for (BlockPos a : d.shape.frameAreaWithoutCorner) {
						for (DiligentMatcher.TransformedShape matchableShapeVariant : matchable) {
							if (matchableShapeVariant.scale != 1) continue;
							BlockPortalShape template = matchableShapeVariant.transformedShape;
							BlockPortalShape matched = template.matchShapeWithMovedFirstFramePos(
									posx -> air.test(world2.getBlockState(posx)),
									posx -> frame.test(world2.getBlockState(posx)),
									a,
									temp2
									);
							if (matched != null) {
								if (world != world2 || !shape.anchor.equals(matched.anchor)) {
									(d.inUse.isBlank() ? list : inUse).add(d);
									for (String y : d.matching) {
										DataStorage e = this.getDataWithID(y);
										if (e != null) {
											(e.inUse.isBlank() ? list : inUse).add(e);
										}
									}
									w = false;
									break k;
								}
							}
						}
					}
				}
			}
		list.addAll(inUse);
		inUse.clear();
		return list;
	}
	
	public List<DataStorage> getMatchingFramesFromBlockPos(BlockPos pos, ResourceKey<Level> dimension, Player player) {
		List<DataStorage> d = new ArrayList<DataStorage>();
		List<DataStorage> de = new ArrayList<DataStorage>();
		DataStorage x = this.getDataWithPos(pos, dimension);
		if (x == null) {return d;}
		if (!x.inUse.isBlank()) {
			DataStorage e = this.getDataWithID(x.inUse);
			if (e != null) {
				d.add(e);
			}
		}
		for (String s : x.matching) {
			DataStorage j = getDataWithID(s);
			if (j != null && j.canPlayerAccess(player) && !d.contains(j) && !de.contains(j)) {
				(j.inUse.isBlank() ? d : de).add(j);
			}
		}
		d.addAll(de);
		de.clear();
		return d;
	}
	
	public DataStorage getDataWithID(String ID) {
		for (DataStorage d : data) {
			if (d.ID.equals(ID)) {
				return d;
			}
		}
		return null;
	}
	
	public DataStorage getDataWithPos(BlockPos pos, ResourceKey<Level> dimension) {
		for (DataStorage d : data) {
			if (d.controllerPos.equals(pos) && d.dimension.compareTo(dimension) == 0) {
				return d;
			}
		}
		return null;
	}
	
	public void updateDataStorage(Var type, String ID, Consumer<DataStorage> c) {
		DataStorage d = getDataWithID(ID);
		if (d != null) {
			c.accept(d);
			sendSpecificUpdate(ID, type);
		}
	}
	
	public void sendFullUpdate() {
		if (!PortalUtils.isRunningOnClient()) {return;}
		for (StorageEmittee e : emittees) {
			e.fullUpdate();
		}
	}
	
	public void sendSpecificUpdate(String ID, Var type) {
		if (!PortalUtils.isRunningOnClient()) {return;}
		for (StorageEmittee e : emittees) {
			e.updateSpecific(type, getDataWithID(ID));
		}
	}
	
	public void addEmittee(StorageEmittee e) {
		emittees.add(e);
	}
	
	public void removeEmittee(StorageEmittee e) {
		emittees.remove(e);
	}
	
	public boolean checkFrame(String ID) {
		DataStorage e = this.getDataWithID(ID);
		if (e != null) {
			return checkFrameFromPortalBlock(e.controllerPos, e.dimension);
		}
		return false;
	}
	
	public boolean checkFrameFromPortalBlock(BlockPos pos, ResourceKey<Level> dimension) {
		if (PortalUtils.isRunningOnClient()) {return false;}
		DataStorage s = this.getDataWithPos(pos, dimension);
		if (s == null) {return false;} //make it remove the controllerpos?
		ServerLevel level = PortalUtils.getServerLevelFromDimensionKey(dimension);
		if (level.getBlockState(s.controllerPos).getBlock() != PortalContent.PORTAL_CONTROLLER.get() || PortalUtils.findPortalShape(level, s.controllerPos, s.dir) == null) {
			this.removePortal(s.controllerPos, dimension, true);
			return false;
		}
		return true;
	}
	
	public void connectFrames(String ID, BlockPos pos, ResourceKey<Level> dimension, ServerPlayer player) {
		connectFrames(ID, getDataWithPos(pos, dimension).ID, PortalUtils.getPredicates(true), PortalUtils.getPredicates(false), player);
	}
	
	public void connectFrames(String ID, BlockPos pos, ResourceKey<Level> dimension, ServerPlayer player, Predicate<BlockState> air, Predicate<BlockState> frame) {
		connectFrames(ID, getDataWithPos(pos, dimension).ID, air, frame, player);
	}
	
	public void connectFrames(String IDa, String IDb, Predicate<BlockState> air, Predicate<BlockState> frame, ServerPlayer player) {
		DataStorage a = getDataWithID(IDa);
		DataStorage b = getDataWithID(IDb);
		if (a == null || b == null || !a.canPlayerAccess(player) || !b.canPlayerAccess(player)) {
			return;
		}
		if (!a.inUse.isBlank()) {
			DataStorage e = this.getDataWithID(a.inUse);
			if (e != null && !e.canPlayerAccess(player)) return;
		}
		if (!b.inUse.isBlank()) {
			DataStorage e = this.getDataWithID(b.inUse);
			if (e != null && !e.canPlayerAccess(player)) return;
		}
		PortalGenInfo po = null;
		ServerLevel world = PortalUtils.getServerLevelFromDimensionKey(a.dimension);
		ServerLevel world2 = PortalUtils.getServerLevelFromDimensionKey(b.dimension);
		List<DiligentMatcher.TransformedShape> matchable = DiligentMatcher.getMatchableShapeVariants(a.shape, 20);
		BlockPos.MutableBlockPos temp2 = new BlockPos.MutableBlockPos();
		for (BlockPos ae : b.shape.frameAreaWithoutCorner) {
			for (DiligentMatcher.TransformedShape matchableShapeVariant : matchable) {
				if (matchableShapeVariant.scale != 1) continue;
				BlockPortalShape template = matchableShapeVariant.transformedShape;
				BlockPortalShape matched = template.matchShapeWithMovedFirstFramePos(
						posx -> air.test(world2.getBlockState(posx)),
						posx -> frame.test(world2.getBlockState(posx)),
						ae,
						temp2
						);
				if (matched != null) {
					if (world != world2 || !a.shape.anchor.equals(matched.anchor)) {
						po = new PortalGenInfo(
							a.dimension,
							b.dimension,
							a.shape, b.shape,
							matchableShapeVariant.rotation.toQuaternion(),
							matchableShapeVariant.scale
						);
					}
				}
			}
		}
		if (po == null) {return;}
		if (!a.inUse.isBlank()) {
			removePortal(IDa, false);
		}
		if (!b.inUse.isBlank()) {
			removePortal(IDb, false);
		}
		LinkedPortal p = po.createTemplatePortal(LinkedPortal.entityType);
		p.original = true;
		LinkedPortal be = PortalManipulation.createFlippedPortal(p, LinkedPortal.entityType);
		LinkedPortal ce = PortalManipulation.createReversePortal(p, LinkedPortal.entityType);
		ce.original = true;
		LinkedPortal de = PortalManipulation.createFlippedPortal(ce, LinkedPortal.entityType);
		PortalControllerEntity pe = null;
		BlockPos controller1Pos = null;
		BlockPos controller2Pos = null;
		for (BlockPos ae : a.shape.frameAreaWithoutCorner) {
			if (world.getBlockState(ae).getBlock() == PortalContent.PORTAL_CONTROLLER.get()) {
				if (controller1Pos != null) {
					removePortals(p,be,ce,de);
					return;
				}
				pe = (PortalControllerEntity) world.getBlockEntity(ae);
				if (pe.getPortal() != null) {
					removePortals(p,be,ce,de);
					return;
				}
				controller1Pos = ae;
			}
		}
		PortalControllerEntity pe2 = null;
		for (BlockPos ae : b.shape.frameAreaWithoutCorner) {
			if (world2.getBlockState(ae).getBlock() == PortalContent.PORTAL_CONTROLLER.get()) {
				if (controller2Pos != null) {
					removePortals(p,be,ce,de);
					return;
				}
				pe2 = (PortalControllerEntity) world2.getBlockEntity(ae);
				if (pe2.getPortal() != null) {
					removePortals(p,be,ce,de);
					return;
				}
				controller2Pos = ae;
			}
		}
		if (pe == null || pe2 == null) {
			removePortals(p,be,ce,de);
			return;
		} else {
			pe.setPortal(p);
			p.controllerPos = controller1Pos;
			pe.setControllerForAll(a.shape);
			pe2.setPortal(ce);
			ce.controllerPos = controller2Pos;
			pe2.setControllerForAll(b.shape);
		}
		
		if (!a.specific_player.isBlank()) {
			p.specificPlayer = a.specific_player;
			be.specificPlayer = a.specific_player;
		}
		if (!b.specific_player.isBlank()) {
			ce.specificPlayer = b.specific_player;
			de.specificPlayer = b.specific_player;
		}
		PortalExtension.get(p).bindCluster = true;
		p.blockPortalShape = a.shape;
		be.blockPortalShape = a.shape;
		ce.blockPortalShape = b.shape;
		de.blockPortalShape = b.shape;

		p.reversePortalId = ce.getUUID();
		ce.reversePortalId = p.getUUID();
		be.reversePortalId = de.getUUID();
		de.reversePortalId = be.getUUID();
		
		PortalAPI.spawnServerEntity(p);
		PortalAPI.spawnServerEntity(be);
		PortalAPI.spawnServerEntity(ce);
		PortalAPI.spawnServerEntity(de);
		
		p.isLightSource = a.isLightSource;
		ce.isLightSource = b.isLightSource;
		p.createOrDestroyPlaceholders();
		ce.createOrDestroyPlaceholders();
		
		a.inUse = b.ID;
		b.inUse = a.ID;
		a.portalUUID = p.getUUID();
		b.portalUUID = ce.getUUID();
		a.portal = p;
		b.portal = ce;
		a.matching.remove(IDb);
		a.matching.add(0, IDb);
		b.matching.remove(IDa);
		b.matching.add(0, IDa);
		syncSpecificVarToPlayers(Var.IN_USE, a, null);
		syncSpecificVarToPlayers(Var.IN_USE, b, null);
		setDirty(true);
	}
	
	public static void removePortals(LinkedPortal a, LinkedPortal b, LinkedPortal c, LinkedPortal d) {
		a.makeWay();
		b.makeWay();
		c.makeWay();
		d.makeWay();
	}
}
