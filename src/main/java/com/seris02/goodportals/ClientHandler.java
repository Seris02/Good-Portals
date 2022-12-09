package com.seris02.goodportals;

import java.util.Map;
import java.util.function.Function;

import com.seris02.goodportals.blocks.PortalBakedModel;
import com.seris02.goodportals.datagen.PortalContent;
import com.seris02.goodportals.renderers.BlockEntityRenderDelegate;
import com.seris02.goodportals.renderers.CamoBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = GoodPortals.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {
	public static final BlockEntityRenderDelegate CAMO_RENDER_DELEGATE = new BlockEntityRenderDelegate();
	
	private static LazyOptional<Block[]> camoBlocks = LazyOptional.of(() -> new Block[] {
			PortalContent.PORTAL_BLOCK.get(),
			PortalContent.PORTAL_CONTROLLER.get()
	});
	
	public static void putCamoBeRenderer(BlockEntity disguisableBlockEntity, BlockState state) {
		CAMO_RENDER_DELEGATE.putDelegateFor(disguisableBlockEntity, state);
	}
	
	public static void refreshModelData(BlockEntity be) {
		BlockPos pos = be.getBlockPos();

		ModelDataManager.requestModelDataRefresh(be);
		Minecraft.getInstance().levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
	}
	
	@SubscribeEvent
	public static void onModelBakingCompleted(ModelBakeEvent event) {
		Map<ResourceLocation, BakedModel> modelRegistry = event.getModelRegistry();

		for (Block block : camoBlocks.orElse(null)) {
			for (BlockState state : block.getStateDefinition().getPossibleStates()) {
				registerCamoModel(modelRegistry, PortalBakedModel::new, state);
				
			}
		}
	}
	
	public static String getstatestring(BlockState state) {
		String string = state.toString();
		string.substring(1, string.length() - 1);
		return string;
	}
	
	public static ResourceLocation getRegistryName(Block block) {
		return ForgeRegistries.BLOCKS.getKey(block);
	}
	
	private static void registerCamoModel(Map<ResourceLocation, BakedModel> modelRegistry, Function<BakedModel, BakedModel> creator, BlockState state) {
		modelRegistry.put(BlockModelShaper.stateToModelLocation(state), creator.apply(modelRegistry.get(BlockModelShaper.stateToModelLocation(state))));
	}
}
