package com.seris02.goodportals.gui;

import java.util.ArrayList;
import java.util.List;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.seris02.goodportals.GoodPortals;
import com.seris02.goodportals.PortalUtils;
import com.seris02.goodportals.blocks.PortalControllerEntity;
import com.seris02.goodportals.connection.PortalInfoPacket;
import com.seris02.goodportals.connection.PortalInfoPacket.ToDo;
import com.seris02.goodportals.connection.RefreshCamoModel;
import com.seris02.goodportals.storage.DataStorage;
import com.seris02.goodportals.storage.DataStorage.Var;
import com.seris02.goodportals.storage.PortalStorage;
import com.seris02.goodportals.storage.StorageEmittee;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class PortalStorageScreen extends Screen implements StorageEmittee {
	public class ScrollButton extends Button {
		private static final ResourceLocation SCROLL = new ResourceLocation(GoodPortals.MODID, "textures/gui/scroll.png");
		private boolean up;
		public ScrollButton(int p_93721_, int p_93722_, int p_93723_, int p_93724_, Component p_93725_, boolean up, OnPress p_93726_) {
			super(p_93721_, p_93722_, p_93723_, p_93724_, p_93725_, p_93726_);
			this.up = up;
		}
		public void renderButton(PoseStack stack, int p_93677_, int p_93678_, float p_93679_) {
			super.renderButton(stack, p_93677_, p_93678_, p_93679_);
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
		    RenderSystem.setShaderTexture(0, SCROLL);
		    this.blit(stack, this.x + this.width / 2 - 5, this.y + this.height / 2 - 3 - (up ? 1 : 0), up ? 11 : 0, 0, 11, 7, 22, 7);
		}
	}
	
	/*
	 * blit(stack, leftpos, topPos, textureleftpos, texturetoppos, texturewidth, textureheight, (png width), (png height))
	 */
	private static final ResourceLocation GUI = new ResourceLocation(GoodPortals.MODID, "textures/gui/screen.png");
	private final int imageWidth = 347;
	private final int imageHeight = 234;
	public LocalPlayer player;
	public BlockPos pos;
	public PortalControllerEntity controller;
	public EditBox addName;
	public Button addButton;
	private ResourceKey<Level> dimension;
	private boolean hasData = false;
	public Button renameButton;
	public Button deleteButton;
	public boolean areyousure = false;
	public int scroll = 0;
	public List<Button> toClear;
	public List<DataStorage> data;
	public DataStorage mine;
	public Button scrollUp;
	public Button scrollDown;
	
	public List<Button> rightSideButtons;
	public Button playerChange;
	public Button teleportself;
	public Button renderplayers;
	public Button isLightSource;

	public PortalStorageScreen(LocalPlayer player, BlockPos pos, PortalControllerEntity controller) {
		super(Component.literal("Portal Controller Screen"));
		this.player = player;
		this.pos = pos;
		this.controller = controller;
		this.dimension = controller.getLevel().dimension();
		PortalStorage.get().addEmittee(this);
		toClear = new ArrayList<Button>();
		rightSideButtons = new ArrayList<Button>();
	}
	
	public boolean isPauseScreen() {
		return false;
	}
	
	public void onClose() {
		PortalStorage.get().removeEmittee(this);
		super.onClose();
	}
	
	protected void init() {
		Component a = Component.literal("+");
		int relX = (this.width - this.imageWidth) / 2;
		int relY = (this.height - this.imageHeight) / 2;
		int textwidth = 235;
		this.addName = new EditBox(this.font, relX + (this.imageWidth - (textwidth + 25)) / 2, relY - 25, textwidth, 20, Component.literal("Set Portal Name"));
		this.addName.setMaxLength(40);
		this.addName.setSuggestion("Set Portal Name");
		this.addName.setResponder(this::onEdited);
		this.addRenderableWidget(this.addName);
		this.addButton = new Button(this.addName.x + this.addName.getWidth() + 5, this.addName.y, 20, 20, a, (p_211790_) -> {
			unsure();
			if (this.addName.getValue().isBlank()) {
				return;
			}
			GoodPortals.channel.send(PacketDistributor.SERVER.with(null), new PortalInfoPacket(pos, controller.getLevel().dimension(), this.addName.getValue(), ToDo.NEW_FRAME));
		});
		this.addRenderableWidget(addButton);
		renameButton = new Button(this.addName.x + this.addName.getWidth() + 5, this.addName.y, 50, 20, Component.literal("Rename"), (button) -> {
			unsure();
			PortalUtils.sendToServer(new PortalInfoPacket(pos, controller.getLevel().dimension(), this.addName.getValue(), ToDo.RENAME));
		});
		deleteButton = new Button(this.renameButton.x + this.renameButton.getWidth() + 5, this.addName.y, 50, 20, Component.literal("Delete"), (button) -> {
			if (areyousure) {
				unsure();
				PortalUtils.sendToServer(PortalInfoPacket.removePortalPacket(mine.ID, true));
			} else {
				areyousure = true;
				Component d = Component.literal("Sure?");
				d.toFlatList(d.getStyle().withColor(ChatFormatting.RED));
				deleteButton.setMessage(d);
			}
		});
		renameButton.visible = false;
		deleteButton.visible = false;
		this.addRenderableWidget(renameButton);
		this.addRenderableWidget(deleteButton);
		scrollUp = new ScrollButton(relX + 5 + 210, (relY+7), 20, 20, Component.literal(""), true, (button) -> {
			unsure();
			scroll = Math.max(0, scroll - (hasShiftDown() ? 5 : 1));
			refreshPortalButtons();
		});
		scrollDown = new ScrollButton(relX + 5 + 210, relY+207, 20, 20, Component.literal(""), false, (button) -> {
			unsure();
			scroll = Math.min(scroll + (hasShiftDown() ? 5 : 1), data.size() - 11);
			refreshPortalButtons();
		});
		this.addRenderableWidget(scrollUp);
		this.addRenderableWidget(scrollDown);
		int startx = 5 + 210 + 25;
		int widthrighthand = this.imageWidth - startx - 7;
		playerChange = new Button(relX + startx, relY+7, widthrighthand, 20, Component.literal("Lock to Player"), (button) -> {
			unsure();
			PortalUtils.sendToServer(PortalInfoPacket.playerChangePacket(mine.ID, mine.specific_player.isBlank()));
		});
		Button axis1 = new Button(relX + startx, (relY+7 + 25), widthrighthand, 20, Component.literal("Flip Portal"), (button) -> {
			unsure();
			PortalUtils.sendToServer(PortalInfoPacket.axisFlipPacket(pos, dimension, true));
		});
		Button axis2 = new Button(relX + startx, (relY+7 + 50), widthrighthand, 20, Component.literal("Rotate Portal"), (button) -> {
			unsure();
			PortalUtils.sendToServer(PortalInfoPacket.axisFlipPacket(pos, dimension, false));
		});
		renderplayers = new Button(relX + startx, (relY+7 + 50), widthrighthand, 20, Component.literal("Renders Players"), (button) -> {
			unsure();
			PortalUtils.sendToServer(PortalInfoPacket.changeRenderSelfPacket(mine.ID, !mine.renderPlayers));
		});
		teleportself = new Button(relX + startx, (relY+7 + 50), widthrighthand, 20, Component.literal("Teleports All"), (button) -> {
			unsure();
			PortalUtils.sendToServer(PortalInfoPacket.onlyTeleportSelfPacket(mine.ID, !mine.onlyTeleportSelf));
		});
		isLightSource = new Button(relX + startx, (relY+7 + 50), widthrighthand, 20, Component.literal("Lit Up"), (button) -> {
			unsure();
			PortalUtils.sendToServer(PortalInfoPacket.setIsLightSourcePacket(mine.ID, !mine.isLightSource));
		});
		rightSideButtons.add(playerChange);
		rightSideButtons.add(teleportself);
		rightSideButtons.add(axis1);
		rightSideButtons.add(axis2);
		rightSideButtons.add(renderplayers);
		rightSideButtons.add(isLightSource);
		rightSideButtons.forEach((button) -> this.addRenderableWidget(button));
		refreshPortals();
	}
	
	private void unsure() {
		areyousure = false;
		Component d = Component.literal("Delete");
		d.toFlatList();
		deleteButton.setMessage(d);
	}
	
	private void disOrAbleScroll() {
		scroll = Math.min(scroll, data.size() - 11);
		scroll = Math.max(0, scroll);
		scrollDown.active = data == null ? false : scroll < data.size() - 11;
		scrollUp.active = data == null ? false : scroll > 0;
	}
	
	private void recalcPositions() {
		int relX = (this.width - this.imageWidth) / 2;
		if (hasData) {
			addName.x = relX + (this.imageWidth - (235 + this.renameButton.getWidth() + 5 + this.deleteButton.getWidth() + 5)) / 2;
			addButton.visible = false;
			renameButton.x = this.addName.x + this.addName.getWidth() + 5;
			renameButton.visible = true;
			deleteButton.x = this.renameButton.x + this.renameButton.getWidth() + 5;
			deleteButton.visible = true;
		} else {
			addName.x = relX + (this.imageWidth - (235 + this.addButton.getWidth() + 5)) / 2;
			addButton.x = this.addName.x + this.addName.getWidth() + 5;
			addButton.visible = true;
			renameButton.visible = false;
			deleteButton.visible = false;
		}
	}
	
	private void refreshPortalButtons() {
		disOrAbleScroll();
		while (toClear.size() > 0) {
			toClear.get(0).visible = false;
			this.removeWidget(toClear.get(0));
			toClear.remove(0);
		}
		int relX = (this.width - this.imageWidth) / 2;
		int relY = (this.height - this.imageHeight) / 2;
		for (int x = scroll; x < Math.min(data.size(), 11+scroll); x++) {
			int posy = x - scroll;
			DataStorage d = data.get(x);
			boolean isConnected = mine != null && d.inUse.equals(mine.ID);
			int cz = font.width(d.name);
			String sd = d.name;
			boolean inUse = !d.inUse.isBlank();
			if (cz > 209 - 5 - (inUse ? 20 : 0)) {
				sd = font.substrByWidth(FormattedText.of(d.name), 209 - 5 - (d.inUse.isBlank() ? 0 : 20) - font.width("...")).getString() + "...";
			}
			Component be = Component.literal(sd);
			TextColor ae = TextColor.fromLegacyFormat(d.inUse.length() > 0 ? (isConnected ? (d.canPlayerAccess(player) ? ChatFormatting.GREEN : ChatFormatting.YELLOW) : ChatFormatting.RED) : ChatFormatting.WHITE);
			be.toFlatList(be.getStyle().withColor(ae));
			Button b = new Button(relX + 5, (relY+7)+20*posy, 209 - (d.inUse.isBlank() ? 0 : 19), 20, be, (button) -> {
				PortalUtils.sendToServer(new PortalInfoPacket(pos, controller.getLevel().dimension(), d.ID, ToDo.CONNECT));
			});
			b.active = d.inUse.length() == 0;
			this.addRenderableWidget(b);
			toClear.add(b);
			if (!d.inUse.isBlank()) {
				Component bp = Component.literal("-");
				bp.toFlatList(be.getStyle().withColor(TextColor.fromLegacyFormat(ChatFormatting.RED)));
				Button da = new Button(b.x + b.getWidth() - 2, b.y, 20, 20, bp, (bs) -> {
					unsure();
					PortalUtils.sendToServer(PortalInfoPacket.removePortalPacket(d.ID, false));
				});
				da.visible = false;
				Button c = new Button(b.x + b.getWidth() - 2, b.y, 20, 20, Component.literal("-"), (button) -> {
					unsure();
					da.visible = true;
					button.visible = false;
				});
				this.addRenderableWidget(c);
				this.addRenderableWidget(da);
				toClear.add(c);
				toClear.add(da);
			}
		}
	}
	
	private void refreshOtherButtons() {
		rightSideButtons.forEach((button) -> button.visible = (mine != null && !mine.inUse.isBlank()));
		playerChange.visible = mine != null;
		if (mine != null) {
			teleportself.visible = teleportself.visible && !mine.specific_player.isBlank();
			this.playerChange.setMessage(Component.literal(mine.specific_player.isBlank() ? "Lock To Myself" : "Unlock Frame"));
			this.teleportself.setMessage(Component.literal(mine.onlyTeleportSelf ? "Teleports Me" : "Teleports All"));
			Component e = Component.literal("Renders Players");
			if (!mine.renderPlayers) {
				e.toFlatList(Style.EMPTY.withStrikethrough(true));
			}
			this.renderplayers.setMessage(e);
			this.isLightSource.setMessage(Component.literal(mine.isLightSource ? "Lit up" : "Unlit"));
		}
		int h = 0;
		int relY = (this.height - this.imageHeight) / 2;
		for (Button x : rightSideButtons) {
			x.y = relY + 7 + (h*25);
			if (x.visible) {
				h++;
			}
		}
	}
	
	private void refreshPortals() {
		PortalStorage e = PortalStorage.get();
		data = e.getMatchingFramesFromBlockPos(pos, dimension, player);
		mine = e.getDataWithPos(pos, dimension);
		hasData = mine != null;
		if (mine != null) {
			this.addName.setSuggestion("");
			this.addName.setValue(mine.name);
		}
		recalcPositions();
		refreshPortalButtons();
		refreshOtherButtons();
		setAllButtonsLocked();
	}
	
	private void setAllButtonsLocked() {
		List<Button> d = new ArrayList<Button>();
		d.addAll(toClear);
		d.addAll(rightSideButtons);
		d.add(renameButton);
		d.add(deleteButton);
		d.add(addButton);
		d.add(playerChange);
		d.forEach((button) -> button.active = !button.active ? false : mine == null || mine.canPlayerAccess(player));
	}
	
	private void onEdited(String text) {
		if (text.isBlank()) {
			this.addName.setSuggestion("Set Portal Name");
		} else {
			this.addName.setSuggestion(null);
		}
	}
	
	public boolean mouseClicked(double x, double y, int z) {
		if (mine == null || mine.canPlayerAccess(player)) {
			return super.mouseClicked(x, y, z);
		}
		return false;
	}
	
	@Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}
	
	public void renderBackground (PoseStack matrix) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GUI);
		int relX = (this.width - this.imageWidth) / 2;
		int relY = (this.height - this.imageHeight) / 2;
		this.blit(matrix, relX, relY, 0, 0, this.imageWidth, this.imageHeight, 512, 256);
	}

	@Override
	public void fullUpdate() {
		refreshPortals();
	}

	@Override
	public void updateSpecific(Var type, DataStorage data) {
		refreshPortals();
	}

}
