package com.seris02.goodportals.connection;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.seris02.goodportals.ClientHandler;
import com.seris02.goodportals.blocks.PortalBlockEntity;
import com.seris02.goodportals.blocks.PortalControllerEntity;
import com.seris02.goodportals.storage.DataStorage;
import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.q_misc_util.dimension.DimId;

public class SingleDataStorageRefresh {
	
	private DataStorage.Var toUse;
	private String name;
	private ResourceKey<Level> dimension;
	private String in_use;
	private String ID;
	private BlockPortalShape shape;
	private String specific_player;
	private boolean bool;

	public SingleDataStorageRefresh() {}

	public SingleDataStorageRefresh(String ID, DataStorage.Var type, DataStorage d) {
		this.ID = ID;
		this.toUse = type;
		switch (this.toUse) {
			case NAME:
				this.name = d.name;
				break;
			case DIMENSION:
				this.dimension = d.dimension;
				break;
			case IN_USE:
				this.in_use = d.inUse;
				break;
			case SHAPE:
				this.shape = d.shape;
				break;
			case SPECIFIC_PLAYER:
				this.specific_player = d.specific_player;
				break;
			case RENDER_PLAYER:
				this.bool = d.renderPlayers;
				break;
			case ONLY_TELEPORT_SELF:
				this.bool = d.onlyTeleportSelf;
				break;
			default:
				break;
		}
	}

	public static void encode(SingleDataStorageRefresh message, FriendlyByteBuf buf) {
		buf.writeEnum(message.toUse);
		CompoundTag nbt = new CompoundTag();
		nbt.putString("I", message.ID);
		switch(message.toUse) {
			case NAME:
				nbt.putString("d", message.name);
				break;
			case DIMENSION:
				DimId.putWorldId(nbt, "d", message.dimension);
				break;
			case IN_USE:
				nbt.putString("d", message.in_use);
				break;
			case SHAPE:
				nbt.put("d", message.shape.toTag());
				break;
			case SPECIFIC_PLAYER:
				nbt.putString("d", message.specific_player);
				break;
			case RENDER_PLAYER:
			case ONLY_TELEPORT_SELF:
				nbt.putBoolean("d", message.bool);
				break;
			default:
				break;
		}
		buf.writeNbt(nbt);
	}

	public static SingleDataStorageRefresh decode(FriendlyByteBuf buf) {
		SingleDataStorageRefresh message = new SingleDataStorageRefresh();

		message.toUse = buf.readEnum(DataStorage.Var.class);
		CompoundTag nbt = buf.readNbt();
		message.ID = nbt.getString("I");
		switch(message.toUse) {
			case NAME:
				message.name = nbt.getString("d");
				break;
			case DIMENSION:
				message.dimension = DimId.getWorldId(nbt, "d", true);
				break;
			case IN_USE:
				message.in_use = nbt.getString("d");
				break;
			case SHAPE:
				message.shape = new BlockPortalShape(nbt.getCompound("d"));
			case SPECIFIC_PLAYER:
				message.specific_player = nbt.getString("d");
			case RENDER_PLAYER:
			case ONLY_TELEPORT_SELF:
				message.bool = nbt.getBoolean("d");
				break;
			default:
				break;
		}
		return message;
	}

	public static void onMessage(SingleDataStorageRefresh message, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			PortalStorage.get().updateDataStorage(message.toUse, message.ID, (data) -> {
				PortalControllerEntity pe = data.getControllerEntity();
				switch(message.toUse) {
					case NAME:
						data.name = message.name;
						break;
					case DIMENSION:
						data.dimension = message.dimension;
						break;
					case IN_USE:
						data.inUse = message.in_use;
						break;
					case SHAPE:
						data.shape = message.shape;
						break;
					case SPECIFIC_PLAYER:
						data.specific_player = message.specific_player;
						if (pe != null) {
							if (pe.getPortal() != null) {
								pe.getPortal().setToPlayer(data.specific_player);
							}
						}
						break;
					case RENDER_PLAYER:
						data.renderPlayers = message.bool;
						if (pe != null) {
							if (pe.getPortal() != null) {
								pe.getPortal().setdoRenderPlayer(message.bool);
							}
						}
						break;
					case ONLY_TELEPORT_SELF:
						data.onlyTeleportSelf = message.bool;
						if (pe != null) {
							if (pe.getPortal() != null) {
								pe.getPortal().setToTeleportSelf(message.bool);
							}
						}
						break;
					default:
						break;
				}
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
