package com.seris02.goodportals.connection;

import java.util.function.Supplier;

import com.seris02.goodportals.ClientHandler;
import com.seris02.goodportals.PortalUtils;
import com.seris02.goodportals.blocks.PortalBlockEntity;
import com.seris02.goodportals.blocks.PortalControllerEntity;
import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.util.CertificateHelper;
import net.minecraftforge.network.NetworkEvent;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.dimension.DimId;

public class PortalInfoPacket {
	public enum ToDo {
		NEW_FRAME,
		CONNECT,
		REMOVE,
		RENAME,
		SPECIFIC_PLAYER_CHANGE,
		AXIS_FLIP,
		CHANGE_RENDER_PLAYERS,
		ONLY_TELEPORT_ME,
		IS_LIGHT_SOURCE
	}
	private BlockPos pos;
	private ResourceKey<Level> dim;
	private String name;
	private boolean remove;
	private ToDo todo;
	private boolean way;

	public PortalInfoPacket() {}

	public PortalInfoPacket(BlockPos pos, ResourceKey<Level> dimension, String name, ToDo todo) {
		this.pos = pos;
		this.dim = dimension;
		this.name = name;
		this.todo = todo;
	}
	
	private PortalInfoPacket(String name, ToDo todo) {
		this.name = name;
		this.todo = todo;
	}
	
	public static PortalInfoPacket removePortalPacket(String ID, boolean remove) {
		PortalInfoPacket p = new PortalInfoPacket(null, null, ID, ToDo.REMOVE);
		p.remove = remove;
		return p;
	}
	
	public static PortalInfoPacket playerChangePacket(String ID, boolean on) {
		PortalInfoPacket p = new PortalInfoPacket(ID, ToDo.SPECIFIC_PLAYER_CHANGE);
		p.way = on;
		return p;
	}

	public static PortalInfoPacket axisFlipPacket(BlockPos pos, ResourceKey<Level> dimension, boolean way) {
		PortalInfoPacket p = new PortalInfoPacket(pos, dimension, null, ToDo.AXIS_FLIP);
		p.way = way;
		return p;
	}

	public static PortalInfoPacket changeRenderSelfPacket(String ID, boolean way) {
		PortalInfoPacket p = new PortalInfoPacket(null, null, ID, ToDo.CHANGE_RENDER_PLAYERS);
		p.way = way;
		return p;
	}
	
	public static PortalInfoPacket onlyTeleportSelfPacket(String ID, boolean way) {
		PortalInfoPacket p = new PortalInfoPacket(null, null, ID, ToDo.ONLY_TELEPORT_ME);
		p.way = way;
		return p;
	}
	
	public static PortalInfoPacket setIsLightSourcePacket(String ID, boolean way) {
		PortalInfoPacket p = new PortalInfoPacket(null, null, ID, ToDo.IS_LIGHT_SOURCE);
		p.way = way;
		return p;
	}

	public static void encode(PortalInfoPacket message, FriendlyByteBuf buf) {
		buf.writeEnum(message.todo);
		if (message.todo != ToDo.AXIS_FLIP) {
			CompoundTag t = new CompoundTag();
			t.putString("s", message.name);
			buf.writeNbt(t);
			switch(message.todo) {
				case SPECIFIC_PLAYER_CHANGE:
				case CHANGE_RENDER_PLAYERS:
				case ONLY_TELEPORT_ME:
				case IS_LIGHT_SOURCE:
					buf.writeBoolean(message.way);
					return;
				default:
					break;
			}
		} else {
			buf.writeBoolean(message.way);
		}
		if (message.todo == ToDo.REMOVE) {
			buf.writeBoolean(message.remove);
			return;
		}
		buf.writeBlockPos(message.pos);
		DimId.writeWorldId(buf, message.dim, true);
	}

	public static PortalInfoPacket decode(FriendlyByteBuf buf) {
		PortalInfoPacket message = new PortalInfoPacket();
		
		message.todo = buf.readEnum(ToDo.class);
		if (message.todo != ToDo.AXIS_FLIP) {
			CompoundTag t = buf.readNbt();
			message.name = t.getString("s");
			switch(message.todo) {
				case SPECIFIC_PLAYER_CHANGE:
				case CHANGE_RENDER_PLAYERS:
				case ONLY_TELEPORT_ME:
				case IS_LIGHT_SOURCE:
					message.way = buf.readBoolean();
					return message;
				default:
					break;
			}
		} else {
			message.way = buf.readBoolean();
		}
		if (message.todo == ToDo.REMOVE) {
			message.remove = buf.readBoolean();
			return message;
		}
		message.pos = buf.readBlockPos();
		message.dim = DimId.readWorldId(buf, false);
		return message;
	}

	public static void onMessage(PortalInfoPacket message, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			switch(message.todo) {
				case NEW_FRAME:
					PortalStorage.get().addFrame(message.pos, message.dim, message.name);
					break;
				case CONNECT:
					PortalStorage.get().connectFrames(message.name, message.pos, message.dim, ctx.get().getSender());
					break;
				case REMOVE:
					PortalStorage.get().removePortal(message.name, message.remove, ctx.get().getSender());
					break;
				case RENAME:
					PortalStorage.get().renamePortal(message.pos, message.dim, message.name, ctx.get().getSender());
					break;
				case SPECIFIC_PLAYER_CHANGE:
					PortalStorage.get().setToPlayer(message.name, ctx.get().getSender(), message.way);
					break;
				case CHANGE_RENDER_PLAYERS:
					PortalStorage.get().setRenderSelf(message.name, ctx.get().getSender(), message.way);
					break;
				case ONLY_TELEPORT_ME:
					PortalStorage.get().setOnlyTeleportSelf(message.name, ctx.get().getSender(), message.way);
					break;
				case IS_LIGHT_SOURCE:
					PortalStorage.get().setIsLightSource(message.name, ctx.get().getSender(), message.way);
					break;
				case AXIS_FLIP:
					PortalControllerEntity pe = PortalStorage.get().getDataWithPos(message.pos, message.dim).getControllerEntity();
					if (pe instanceof PortalControllerEntity) {
						if (pe.getPortal() != null) {
							pe.getPortal().flipPortalAxis(message.way);
						}
					}
					break;
				}
		});

		ctx.get().setPacketHandled(true);
	}
}
