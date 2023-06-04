package com.seris02.goodportals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.seris02.goodportals.blocks.PortalBlockEntity;
import com.seris02.goodportals.connection.PortalInfoPacket;
import com.seris02.goodportals.connection.PortalStorageRefresh;
import com.seris02.goodportals.connection.RefreshCamoModel;
import com.seris02.goodportals.connection.SingleDataStorageRefresh;
import com.seris02.goodportals.datagen.PortalContent;
import com.seris02.goodportals.gui.PortalStorageScreen;
import com.seris02.goodportals.storage.DataStorage;
import com.seris02.goodportals.storage.PortalStorage;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import qouteall.imm_ptl.core.render.PortalEntityRenderer;
import net.minecraft.world.entity.player.Player;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(GoodPortals.MODID)
@EventBusSubscriber(modid = GoodPortals.MODID, bus = Bus.MOD)
public class GoodPortals {
	public static final  String MODID = "goodportals";
	// Directly reference a slf4j logger
	private static final Logger LOGGER = LogUtils.getLogger();
	public static SimpleChannel channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> "v1.0", "v1.0"::equals, "v1.0"::equals);
	
	public GoodPortals() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		PortalContent.register(modBus);
		//Mod.EventBusSubscriber.Bus.MOD
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(this::initPortalRenderers);
		});
		MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedInEvent);
		//MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeftClickBlock);
		// Register the setup method for modloading
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		
		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	
    public void initPortalRenderers(EntityRenderersEvent.RegisterRenderers event) {
		Arrays.stream(new EntityType<?>[]{
			PortalContent.LINKED_PORTAL.get()
			}).peek(
				Validate::notNull
			).forEach(
				entityType -> event.registerEntityRenderer(entityType, (EntityRendererProvider) PortalEntityRenderer::new
			));
	}
	
	private void setup(final FMLCommonSetupEvent event) {
		// some preinit code
		GoodPortals.channel.registerMessage(0, RefreshCamoModel.class, RefreshCamoModel::encode, RefreshCamoModel::decode, RefreshCamoModel::onMessage);
		GoodPortals.channel.registerMessage(1, PortalInfoPacket.class, PortalInfoPacket::encode, PortalInfoPacket::decode, PortalInfoPacket::onMessage);
		GoodPortals.channel.registerMessage(2, PortalStorageRefresh.class, PortalStorageRefresh::encode, PortalStorageRefresh::decode, PortalStorageRefresh::onMessage);
		GoodPortals.channel.registerMessage(3, SingleDataStorageRefresh.class, SingleDataStorageRefresh::encode, SingleDataStorageRefresh::decode, SingleDataStorageRefresh::onMessage);
	}
	
	public void onPlayerLoggedInEvent(PlayerLoggedInEvent event) {
		ServerPlayer s = (ServerPlayer) event.getEntity();
		PortalStorage.get().syncToPlayer(s);
	}
	/*
	public void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		Level l = event.getWorld();
		if (l.getBlockEntity(event.getPos()) instanceof PortalBlockEntity pe) {
			if (pe.controllerPos != null) {
				DataStorage e = PortalStorage.get().getDataWithPos(pe.controllerPos, l.dimension());
				DataStorage r = e == null ? null : PortalStorage.get().getDataWithID(e.inUse);
				if ((e != null && !e.canPlayerAccessNoNull(event.getPlayer())) || (r != null && !r.canPlayerAccessNoNull(event.getPlayer()))) {
					System.out.println(event.getPlayer());
					event.setCanceled(true);
				}
			}
		}
	}
	public void onBreakEvent(BreakEvent event) {
		LevelAccessor l = event.getWorld();
		if (l.getBlockEntity(event.getPos()) instanceof PortalBlockEntity pe) {
			if (pe.controllerPos != null) {
				DataStorage e = PortalStorage.get().getDataWithPos(pe.controllerPos, pe.getLevel().dimension());
				DataStorage r = e == null ? null : PortalStorage.get().getDataWithID(e.inUse);
				if ((e != null && !e.canPlayerAccess(event.getPlayer())) || (r != null && !r.canPlayerAccess(event.getPlayer()))) {
					System.out.println(event.getPlayer());
					event.setCanceled(true);
				}
			}
		}
	}*/
}
