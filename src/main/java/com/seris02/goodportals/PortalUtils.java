package com.seris02.goodportals;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.seris02.goodportals.datagen.PortalContent;
import com.seris02.goodportals.storage.DataStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.platform_specific.IPRegistry;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalGeneration;
import qouteall.q_misc_util.MiscHelper;

public class PortalUtils {

	public static BlockPortalShape findPortalShape(ServerLevel level, BlockPos pos, Direction dir, Predicate<BlockState> air, Predicate<BlockState> frame) {
		if (dir == null) dir = Direction.DOWN;
		Block toCheck = level.getBlockState(pos.relative(dir)).getBlock();
		if (toCheck == Blocks.AIR || toCheck == IPRegistry.NETHER_PORTAL_BLOCK.get()) {
			BlockPortalShape from = NetherPortalGeneration.findFrameShape(level, pos.relative(dir), air, frame);
			if (from != null) return from;
		}
		return null;
	}
	public static BlockPortalShape findPortalShape(ServerLevel level, BlockPos pos, Direction dir) {
		return findPortalShape(level, pos, dir, getPredicates(true), getPredicates(false));
	}
	
	public static DataStorage findDataStorage(ServerLevel level, BlockPos pos, String name, List<DataStorage> data) {
		return PortalUtils.findDataStorage(level, pos, name, data, getPredicates(true), getPredicates(false));
	}
	
	public static int sort(BlockPos a, BlockPos b, Vec3 axis) {
		int x = 0;
		if (axis.x > 0) {
			x += a.getX() < b.getX() ? -1 : a.getX() == b.getX() ? 0 : 1;
		}
		if (axis.y > 0) {
			x += a.getY() < b.getY() ? -1 : a.getY() == b.getY() ? 0 : 1;
		}
		if (axis.z > 0) {
			x += a.getZ() < b.getZ() ? -1 : a.getZ() == b.getZ() ? 0 : 1;
		}
		return x;
	}
	
	public static CompoundTag getCheapTagFromShape(BlockPortalShape e) {
		CompoundTag t = new CompoundTag();
		Optional<BlockPos> l = e.area.stream().findAny();
		if (!e.isRectangle() || l.isEmpty()) {
			t = e.toTag();
			t.putBoolean("s", true);
			return t;
		}
		t.putBoolean("s", false);
		t.putInt("a", e.axis.ordinal());
		//Z - min X -> max Y
		//Y - min X -> max Z
		//X - min Z -> max Y
		Vec3 n = new Vec3(e.axis == Axis.Z || e.axis == Axis.Y ? 1 : 0, e.axis == Axis.Z || e.axis == Axis.X ? 1 : 0, e.axis == Axis.X || e.axis == Axis.Y ? 1 : 0);
		BlockPos low = e.area.stream().min((pos1, pos2) -> sort(pos1, pos2, n)).orElse(null);
		BlockPos high = e.area.stream().max((pos1, pos2) -> sort(pos1, pos2, n)).orElse(null);
		if (low == null || high == null) {
			t = e.toTag();
			t.putBoolean("s", false);
			return t;
		}
		ListTag x = new ListTag();
		x.add(IntTag.valueOf(low.getX()));
		x.add(IntTag.valueOf(low.getY()));
		x.add(IntTag.valueOf(low.getZ()));
		x.add(IntTag.valueOf(high.getX()));
		x.add(IntTag.valueOf(high.getY()));
		x.add(IntTag.valueOf(high.getZ()));
		t.put("p", x);
		return t;
	}
	
	public static BlockPortalShape getShapeFromCheapTag(CompoundTag tag) {
		if(!tag.contains("s") || tag.getBoolean("s")) {
			return new BlockPortalShape(tag);
		}
		Axis s = Direction.Axis.values()[tag.getInt("a")];
		ListTag x = tag.getList("p", Tag.TAG_INT);
		if (x.size() != 6) {
			return new BlockPortalShape(tag);
		}
		Set<BlockPos> j = new HashSet<BlockPos>();
		//Z - min X -> max Y
		//Y - min X -> max Z
		//X - min Z -> max Y
		//BlockPos a = new BlockPos(x.getInt(0),x.getInt(1),x.getInt(2));
		//BlockPos b = new BlockPos(x.getInt(3),x.getInt(4),x.getInt(5));
		int min1 = s == Axis.Y || s == Axis.Z ? x.getInt(0) : x.getInt(2);
		int max1 = s == Axis.Y || s == Axis.Z ? x.getInt(3) : x.getInt(5);
		int min2 = s == Axis.X || s == Axis.Z ? x.getInt(1) : x.getInt(2);
		int max2 = s == Axis.X || s == Axis.Z ? x.getInt(4) : x.getInt(5);
		int othe = s == Axis.X ? x.getInt(0) : s == Axis.Y ? x.getInt(1) : x.getInt(2);
		for (int c = min1; c <= max1; c++) {
			for (int d = min2; d <= max2; d++) {
				switch(s) {
				case X:
					j.add(new BlockPos(othe, d, c));
					break;
				case Y:
					j.add(new BlockPos(c, othe, d));
					break;
				case Z:
					j.add(new BlockPos(c, d, othe));
					break;
				}
			}
		}
		return new BlockPortalShape(j, s);
	}

	public static DataStorage findDataStorage(ServerLevel level, BlockPos pos, String name, List<DataStorage> data, Predicate<BlockState> air, Predicate<BlockState> frame) {
		Direction[] dirs = {Direction.DOWN, Direction.UP, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};
		for (Direction dir : dirs) {
			boolean f = false;
			for (DataStorage g : data) {
				if (g.shape.area.contains(pos.relative(dir))) {
					f = true;
				}
			}
			if (f) continue; //F
			if (level.getBlockState(pos.relative(dir)).getBlock() == Blocks.AIR) {
				BlockPortalShape from = NetherPortalGeneration.findFrameShape(level, pos.relative(dir), air, frame);
				if (from != null) {
					DataStorage e = new DataStorage(from, pos, level.dimension(), name);
					e.dir = dir;
					return e;
				}
			}
		}
		return null;
	}
	
	public static Predicate<BlockState> getPredicates(boolean air) {
		if (air) {
			return blockState -> blockState.isAir() || blockState.getBlock() == IPRegistry.NETHER_PORTAL_BLOCK.get();
		}
		return blockState -> blockState.getBlock() == PortalContent.PORTAL_BLOCK.get() || blockState.getBlock() == PortalContent.PORTAL_CONTROLLER.get();
	}
	
	public static boolean isRunningOnClient() {
		return !(FMLEnvironment.dist == Dist.DEDICATED_SERVER || Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER);
	}
	
	public static Level getLevelFromDimensionKey(ResourceKey<Level> dimension) {
		if (!isRunningOnClient()) {
			return getServerLevelFromDimensionKey(dimension);
		}
		return getClientLevelFromDimensionKey(dimension);
	}
	
	public static ServerLevel getServerLevelFromDimensionKey(ResourceKey<Level> dimension) {
		return MiscHelper.getServer().getLevel(dimension);
	}
	
	@OnlyIn(Dist.CLIENT)
	public static Level getClientLevelFromDimensionKey(ResourceKey<Level> dimension) {
		return CHelper.getClientWorld(dimension);
	}
	
	public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
		GoodPortals.channel.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
	
	public static <MSG> void sendToServer(MSG message) {
		GoodPortals.channel.sendToServer(message);
	}
}
