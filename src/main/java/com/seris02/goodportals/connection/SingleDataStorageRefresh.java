package com.seris02.goodportals.connection;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.seris02.goodportals.ClientHandler;
import com.seris02.goodportals.LinkedPortal;
import com.seris02.goodportals.PortalUtils;
import com.seris02.goodportals.blocks.PortalBlockEntity;
import com.seris02.goodportals.blocks.PortalControllerEntity;
import com.seris02.goodportals.storage.DataStorage;
import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import qouteall.imm_ptl.core.ducks.IEWorld;
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
	private UUID portal;
	private ResourceKey<Level> dimIn;

	public SingleDataStorageRefresh() {}

	public SingleDataStorageRefresh(String ID, DataStorage.Var type, DataStorage d, LinkedPortal p) {
		this.ID = ID;
		this.toUse = type;
		if (p != null) {
			this.portal = p.getUUID();
			this.dimIn = p.getOriginDim();
		}
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
			case IS_LIGHT_SOURCE:
				this.bool = d.isLightSource;
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
			case IS_LIGHT_SOURCE:
				nbt.putBoolean("d", message.bool);
				break;
			default:
				break;
		}
		if (message.portal != null) {
			nbt.putUUID("p", message.portal);
			DimId.putWorldId(nbt, "e", message.dimIn);
		}
		buf.writeNbt(nbt);
	}

	public static SingleDataStorageRefresh decode(FriendlyByteBuf buf) {
		SingleDataStorageRefresh message = new SingleDataStorageRefresh();

		message.toUse = buf.readEnum(DataStorage.Var.class);
		CompoundTag nbt = buf.readNbt();
		message.ID = nbt.getString("I");
		if (nbt.contains("p")) {
			message.portal = nbt.getUUID("p");
			message.dimIn = DimId.getWorldId(nbt, "e", true);
		}
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
			case IS_LIGHT_SOURCE:
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
				LinkedPortal p = null;
				if (message.portal != null) {
					Level l = PortalUtils.getClientLevelFromDimensionKey(message.dimIn);
					Entity pe = ((IEWorld) l).portal_getEntityLookup().get(message.portal);
					if (pe instanceof LinkedPortal) {
						p = (LinkedPortal) pe;
					}
				}
				switch(message.toUse) {
					case NAME:
						data.name = message.name;
						break;
					case DIMENSION:
						data.dimension = message.dimension;
						break;
					case IN_USE:
						data.inUse = message.in_use;
						data.matching.remove(message.in_use);
						data.matching.add(0, message.in_use);
						break;
					case SHAPE:
						data.shape = message.shape;
						break;
					case SPECIFIC_PLAYER:
						data.specific_player = message.specific_player;
						if (p != null) {
							p.setToPlayer(data.specific_player);
						}
						break;
					case RENDER_PLAYER:
						data.renderPlayers = message.bool;
						if (p != null) {
							p.setdoRenderPlayer(message.bool);
						}
						break;
					case ONLY_TELEPORT_SELF:
						data.onlyTeleportSelf = message.bool;
						if (p != null) {
							p.setToTeleportSelf(message.bool);
						}
						break;
					case IS_LIGHT_SOURCE:
						data.isLightSource = message.bool; //the rest is serverside, just block placing
						break;
					default:
						break;
				}
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
