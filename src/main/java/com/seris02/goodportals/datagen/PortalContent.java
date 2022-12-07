package com.seris02.goodportals.datagen;

import java.util.function.Supplier;

import com.seris02.goodportals.GoodPortals;
import com.seris02.goodportals.LinkedPortal;
import com.seris02.goodportals.blocks.PortalBlock;
import com.seris02.goodportals.blocks.PortalBlockEntity;
import com.seris02.goodportals.blocks.PortalController;
import com.seris02.goodportals.blocks.PortalControllerEntity;

import net.minecraft.data.DataGenerator;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class PortalContent {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GoodPortals.MODID);
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GoodPortals.MODID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, GoodPortals.MODID);
	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, GoodPortals.MODID);
	
	public static final RegistryObject<BlockEntityType<PortalBlockEntity>> PORTAL_ENTITY = BLOCK_ENTITIES.register("portal_entity", () -> BlockEntityType.Builder.of(PortalBlockEntity::new, PortalContent.PORTAL_BLOCK.get()).build(null));
	public static final RegistryObject<BlockEntityType<PortalControllerEntity>> PORTAL_CONTROLLER_ENTITY = BLOCK_ENTITIES.register("portal_controller_entity", () -> BlockEntityType.Builder.of(PortalControllerEntity::new, PortalContent.PORTAL_CONTROLLER.get()).build(null));

	public static final RegistryObject<Block> PORTAL_CONTROLLER = registerBlock("portal_controller", () -> new PortalController(Block.Properties.of(Material.METAL).sound(SoundType.METAL).strength(0.75f, 1200f)));
	public static final RegistryObject<Block> PORTAL_BLOCK = registerBlock("portal_block", () -> new PortalBlock(Block.Properties.of(Material.METAL).sound(SoundType.METAL).strength(0.75f, 1200f)));
	
	public static final RegistryObject<Item> PORTAL_CONTROLLER_CORE = ITEMS.register("portal_controller_core", () -> new Item(itemProp(CreativeModeTab.TAB_MISC)));
	
	public static final RegistryObject<EntityType<LinkedPortal>> LINKED_PORTAL =
			ENTITY_TYPES.register("linked_portal",
					() -> EntityType.Builder.of(LinkedPortal::new, MobCategory.MISC)
							.sized(1.0f, 1.0f)
							.fireImmune()
							.clientTrackingRange(96)
							.build(GoodPortals.MODID + "linked_portal"));
	
	public static void register(IEventBus eventbus) {
		BLOCKS.register(eventbus);
		BLOCK_ENTITIES.register(eventbus);
		ENTITY_TYPES.register(eventbus);
		ITEMS.register(eventbus);
	}
	
	private static <T extends Block>RegistryObject<T> registerBlock(String name, Supplier<T> block) {
		RegistryObject<T> toReturn = BLOCKS.register(name, block);
		registerBlockItem(name, toReturn);
		return toReturn;
	}
	private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
		PortalContent.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
	}
	
	private static final Item.Properties itemProp(CreativeModeTab itemGroup) {
		return new Item.Properties().tab(itemGroup);
	}
	
	
}
