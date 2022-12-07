package com.seris02.goodportals.blocks;

import com.seris02.goodportals.GoodPortals;
import com.seris02.goodportals.LinkedPortal;
import com.seris02.goodportals.PortalUtils;
import com.seris02.goodportals.connection.RefreshCamoModel;
import com.seris02.goodportals.gui.PortalStorageScreen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

public class PortalController extends PortalBlock {

	public PortalController(Properties properties) {
		super(properties);
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PortalControllerEntity(pos, state);
	}
	
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide && player.getItemInHand(hand).isEmpty()) {
			//GoodPortals.channel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new OpenPortalScreenPacket(pos, level.dimension()));
			ClientLevel world = (ClientLevel) level;
			if (level.getBlockEntity(pos) instanceof PortalControllerEntity pe) {
				openPortalScreen(pos, pe);
			}
		}
		/*if (player.getItemInHand(hand).isEmpty()) {
			if (!level.isClientSide) {
				PortalControllerEntity en = (PortalControllerEntity) level.getBlockEntity(pos);
				LinkedPortal p = en.getPortal();
				if (p != null) {
					p.flipPortalAlongAxis();
				}
				return InteractionResult.SUCCESS;
			}
		}*/
		return super.use(state, level, pos, player, hand, hit);
	}
	@OnlyIn(Dist.CLIENT)
	public void openPortalScreen(BlockPos pos, PortalControllerEntity pe) {
		Minecraft.getInstance().setScreen(new PortalStorageScreen(Minecraft.getInstance().player, pos, pe));
	}

}
