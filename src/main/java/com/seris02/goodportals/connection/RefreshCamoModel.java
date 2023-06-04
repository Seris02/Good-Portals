package com.seris02.goodportals.connection;

import java.util.function.Supplier;

import com.seris02.goodportals.ClientHandler;
import com.seris02.goodportals.blocks.PortalBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class RefreshCamoModel {
	private BlockPos pos;
	private ItemStack stack;

	public RefreshCamoModel() {}

	public RefreshCamoModel(BlockPos pos, ItemStack stack) {
		this.pos = pos;
		this.stack = stack;
	}

	public static void encode(RefreshCamoModel message, FriendlyByteBuf buf) {
		buf.writeBlockPos(message.pos);
		buf.writeItem(message.stack);
	}

	public static RefreshCamoModel decode(FriendlyByteBuf buf) {
		RefreshCamoModel message = new RefreshCamoModel();

		message.pos = buf.readBlockPos();
		message.stack = buf.readItem();
		return message;
	}

	public static void onMessage(RefreshCamoModel message, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			PortalBlockEntity pe = (PortalBlockEntity) Minecraft.getInstance().level.getBlockEntity(message.pos);
			System.out.println("debug: WE GOT THE CAMO MESSAGE");
			if (pe != null) {
				if (message.stack.getItem() instanceof BlockItem blockItem)
				pe.camoState = blockItem.getBlock().defaultBlockState();

				ClientHandler.refreshModelData(pe);
				System.out.println("debug: IT IS NOT NULL");
			}
		});

		ctx.get().setPacketHandled(true);
	}
}