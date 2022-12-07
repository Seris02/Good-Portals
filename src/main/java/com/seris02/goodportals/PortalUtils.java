package com.seris02.goodportals;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.function.Predicate;

import com.seris02.goodportals.datagen.PortalContent;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalGeneration;
import qouteall.q_misc_util.MiscHelper;

public class PortalUtils {
	
	public static BlockPortalShape findPortalShape(ServerLevel level, BlockPos pos, Predicate<BlockState> air, Predicate<BlockState> frame) {
		Direction[] dirs = {Direction.DOWN, Direction.UP, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};
		for (Direction dir : dirs) {
			BlockPortalShape from = NetherPortalGeneration.findFrameShape(level, pos.relative(dir), air, frame);
			if (from != null) return from;
		}
		return null;
	}
	public static BlockPortalShape findPortalShape(ServerLevel level, BlockPos pos) {
		return findPortalShape(level, pos, getPredicates(true), getPredicates(false));
	}
	
	public static Predicate<BlockState> getPredicates(boolean air) {
		if (air) {
			return blockState -> blockState.isAir();
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
