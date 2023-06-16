package com.seris02.goodportals.datagen;

import com.seris02.goodportals.GoodPortals;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.data.event.GatherDataEvent;

@Mod.EventBusSubscriber(modid = GoodPortals.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {
	
	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		generator.addProvider(false, new RecipesProvider(generator));
		generator.addProvider(false, new LootTablesProvider(generator));
	}
}
