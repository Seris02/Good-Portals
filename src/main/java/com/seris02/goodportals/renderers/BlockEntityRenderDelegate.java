/*
Mostly taken from SecurityCraft, so, including their license.

THIS LICENSE ONLY APPLIES TO THIS FILE, THE LICENSE IN LICENSE.txt APPLIES TO EVERY OTHER FILE - except "PortalBakedModel.java" - IN THE REPOSITORY.

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

package com.seris02.goodportals.renderers;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityRenderDelegate {
	private final Map<BlockEntity, DelegateRendererInfo> renderDelegates = new HashMap<>();

	public void putDelegateFor(BlockEntity originalBlockEntity, BlockState delegateState) {
		if (renderDelegates.containsKey(originalBlockEntity)) {
			DelegateRendererInfo delegateInfo = renderDelegates.get(originalBlockEntity);

			//the original be already has a delegate block entity of the same type, just update the state instead of creating a whole new be and renderer
			if (delegateInfo.delegateBlockEntity.getBlockState().getBlock() == delegateState.getBlock()) {
				delegateInfo.delegateBlockEntity.setBlockState(delegateState);
				return;
			}
		}

		if (delegateState != null && delegateState.hasBlockEntity()) {
			Minecraft mc = Minecraft.getInstance();
			BlockEntity delegateBe = ((EntityBlock) delegateState.getBlock()).newBlockEntity(BlockPos.ZERO, delegateState);

			if (delegateBe != null) {
				BlockEntityRenderer<?> delegateBeRenderer;

				delegateBe.setLevel(mc.level);
				delegateBeRenderer = mc.getBlockEntityRenderDispatcher().getRenderer(delegateBe);

				if (delegateBeRenderer != null)
					renderDelegates.put(originalBlockEntity, new DelegateRendererInfo(delegateBe, delegateBeRenderer));
			}
		}
	}

	public void removeDelegateOf(BlockEntity originalBlockEntity) {
		renderDelegates.remove(originalBlockEntity);
	}

	public boolean tryRenderDelegate(BlockEntity originalBlockEntity, float partialTicks, PoseStack pose, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		DelegateRendererInfo delegateRendererInfo = renderDelegates.get(originalBlockEntity);

		if (delegateRendererInfo != null) {
			delegateRendererInfo.delegateRenderer().render(delegateRendererInfo.delegateBlockEntity(), partialTicks, pose, buffer, combinedLight, combinedOverlay);
			return true;
		}

		return false;
	}

	@SuppressWarnings("rawtypes")
	private static record DelegateRendererInfo(BlockEntity delegateBlockEntity, BlockEntityRenderer delegateRenderer) {}
}