package com.seris02.goodportals.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seris02.goodportals.ClientHandler;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CamoBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
	public CamoBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

	@Override
	public void render(T be, float partialTicks, PoseStack pose, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
		ClientHandler.CAMO_RENDER_DELEGATE.tryRenderDelegate(be, partialTicks, pose, buffer, combinedLight, combinedOverlay);
	}
}