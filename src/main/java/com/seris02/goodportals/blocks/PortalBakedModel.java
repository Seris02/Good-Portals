/*
Mostly taken from SecurityCraft, so, including their license.

THIS LICENSE ONLY APPLIES TO THIS FILE, THE LICENSE IN "LICENSE.txt" APPLIES TO EVERY OTHER FILE - except "BlockEntityRenderDelegate.java" - IN THE REPOSITORY.

MIT License

Copyright (c) 2022 Geforce

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.seris02.goodportals.blocks;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelData.Builder;
import net.minecraftforge.client.model.data.ModelProperty;

public class PortalBakedModel implements IDynamicBakedModel {
	public static final ModelProperty<BlockState> CAMO_STATE = new ModelProperty<>();
	private final BakedModel oldModel;
	
	public PortalBakedModel(BakedModel oldModel) {
		this.oldModel = oldModel;
	}

	@Override
	public boolean useAmbientOcclusion() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isGui3d() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean usesBlockLight() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCustomRenderer() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData extraData) {
		BlockState camo = extraData.get(CAMO_STATE);
		if (camo != null) {
			Block block = camo.getBlock();
			if (block != Blocks.AIR) {
				BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(camo);
				if (model != null && model != this) {
					return model.getParticleIcon(extraData);
				}
			}
		}
		return oldModel.getParticleIcon(extraData);
	}
	
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (blockEntity != null) {
			if (blockEntity.getBlockState().getBlock() instanceof PortalBlock portal) {
				Optional<BlockState> camoState = portal.getCamoState(level, pos);
				if (camoState.isPresent()) {
					modelData.builder().with(CAMO_STATE, camoState.get());
					return modelData;
				}
			}
		}

		modelData.builder().with(CAMO_STATE, Blocks.AIR.defaultBlockState());
		return modelData;
	}

	@Override
	public ItemOverrides getOverrides() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
		BlockState camo = extraData.get(CAMO_STATE);
		if (camo != null) {
			Block block = camo.getBlock();
			if (block != Blocks.AIR) {
				BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(camo);
				if (model != null && model != this) {
					return model.getQuads(camo, side, rand, extraData, renderType);
				}
			}
		}
		return oldModel.getQuads(state, side, rand, extraData, renderType);
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return oldModel.getParticleIcon();
	}
}
