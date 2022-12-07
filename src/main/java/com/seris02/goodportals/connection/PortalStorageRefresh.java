package com.seris02.goodportals.connection;

import java.util.function.Supplier;

import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PortalStorageRefresh {
	private CompoundTag nbt;

	public PortalStorageRefresh() {}

	public PortalStorageRefresh(CompoundTag nbt) {
		this.nbt = nbt;
	}

	public static void encode(PortalStorageRefresh message, FriendlyByteBuf buf) {
		buf.writeNbt(message.nbt);
	}

	public static PortalStorageRefresh decode(FriendlyByteBuf buf) {
		PortalStorageRefresh message = new PortalStorageRefresh();

		message.nbt = buf.readAnySizeNbt();
		return message;
	}

	public static void onMessage(PortalStorageRefresh message, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			PortalStorage.get().fromNBT(message.nbt);
		});

		ctx.get().setPacketHandled(true);
	}
}
