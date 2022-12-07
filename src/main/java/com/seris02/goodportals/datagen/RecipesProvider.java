package com.seris02.goodportals.datagen;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

public class RecipesProvider extends RecipeProvider {

	public RecipesProvider(DataGenerator generatorIn) {
		super(generatorIn);
	}

	@Override
	protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {
		ShapedRecipeBuilder.shaped(PortalContent.PORTAL_BLOCK.get())
			.pattern(" q ")
			.pattern("qxq")
			.pattern(" q ")
			.define('q', Tags.Items.GEMS_DIAMOND)
			.define('x', Tags.Items.STORAGE_BLOCKS_IRON)
			.unlockedBy("has_portal_controller", has(PortalContent.PORTAL_CONTROLLER_CORE.get()))
			.save(consumer);
		ShapedRecipeBuilder.shaped(PortalContent.PORTAL_CONTROLLER.get())
			.pattern("qxq")
			.pattern("xix")
			.pattern("qxq")
			.define('q', Tags.Items.GEMS_DIAMOND)
			.define('x', Tags.Items.INGOTS_IRON)
			.define('i', PortalContent.PORTAL_CONTROLLER_CORE.get())
			.unlockedBy("has_portal_controller", has(PortalContent.PORTAL_CONTROLLER_CORE.get()))
			.save(consumer);
		ShapedRecipeBuilder.shaped(PortalContent.PORTAL_CONTROLLER_CORE.get())
			.pattern(" x ")
			.pattern("xix")
			.pattern(" x ")
			.define('x', Tags.Items.INGOTS_GOLD)
			.define('i', Tags.Items.STORAGE_BLOCKS_IRON)
			.unlockedBy("has_gold", has(Tags.Items.INGOTS_GOLD))
			.save(consumer);
	}
	
	@Override
	public String getName() {
		return "GoodPortals Recipes";
	}
}