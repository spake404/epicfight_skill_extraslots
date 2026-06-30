package dev.spake404.epicfight.extraslots;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public class ExtraSlotsConfigScreen extends Screen {
	private static final int ROW_FILL = 0x33000000;
	private static final int WRAPPED_LINE_HEIGHT = 10;

	private final Screen parent;
	private int passiveSlots;
	private int moverSlots;
	private int identitySlots;
	private int maxPassiveSlots;
	private int maxMoverSlots;
	private int maxIdentitySlots;
	
	public ExtraSlotsConfigScreen(Minecraft minecraft, Screen parent) {
		super(Component.translatable(ExtraSlotsConfig.key("title")));
		this.minecraft = minecraft;
		this.parent = parent;
		this.passiveSlots = ExtraSlotsConfig.PASSIVE_SLOTS.get();
		this.moverSlots = ExtraSlotsConfig.MOVER_SLOTS.get();
		this.identitySlots = ExtraSlotsConfig.IDENTITY_SLOTS.get();
		this.maxPassiveSlots = ExtraSlotsConfig.MAX_PASSIVE_SLOTS.get();
		this.maxMoverSlots = ExtraSlotsConfig.MAX_MOVER_SLOTS.get();
		this.maxIdentitySlots = ExtraSlotsConfig.MAX_IDENTITY_SLOTS.get();
	}
	
	@Override
	protected void init() {
		int centerX = this.width / 2;
		int startY = this.firstSlotY();
		
		int maxStartY = ExtraSlotsSkillTreeCompat.isLoaded() ? startY : startY + this.slotGroupGap();
		
		if (!ExtraSlotsSkillTreeCompat.isLoaded()) {
			this.addSlotControls(centerX, startY, () -> this.passiveSlots, value -> this.passiveSlots = value, ExtraSlotsConfig.MIN_PASSIVE_SLOTS, this.maxPassiveSlots);
			this.addSlotControls(centerX, startY + 24, () -> this.moverSlots, value -> this.moverSlots = value, ExtraSlotsConfig.MIN_MOVER_SLOTS, this.maxMoverSlots);
			this.addSlotControls(centerX, startY + 48, () -> this.identitySlots, value -> this.identitySlots = value, ExtraSlotsConfig.MIN_IDENTITY_SLOTS, this.maxIdentitySlots);
		}
		
		this.addSlotControls(centerX, maxStartY, () -> this.maxPassiveSlots, value -> {
			this.maxPassiveSlots = value;
			this.passiveSlots = Math.min(this.passiveSlots, this.maxPassiveSlots);
		}, ExtraSlotsConfig.MIN_PASSIVE_SLOTS, ExtraSlotsConfig.HARD_MAX_PASSIVE_SLOTS);
		this.addSlotControls(centerX, maxStartY + 24, () -> this.maxMoverSlots, value -> {
			this.maxMoverSlots = value;
			this.moverSlots = Math.min(this.moverSlots, this.maxMoverSlots);
		}, ExtraSlotsConfig.MIN_MOVER_SLOTS, ExtraSlotsConfig.HARD_MAX_MOVER_SLOTS);
		this.addSlotControls(centerX, maxStartY + 48, () -> this.maxIdentitySlots, value -> {
			this.maxIdentitySlots = value;
			this.identitySlots = Math.min(this.identitySlots, this.maxIdentitySlots);
		}, ExtraSlotsConfig.MIN_IDENTITY_SLOTS, ExtraSlotsConfig.HARD_MAX_IDENTITY_SLOTS);
		
		this.addRenderableWidget(Button.builder(Component.translatable(ExtraSlotsConfig.key("reset_defaults")), button -> {
			this.passiveSlots = ExtraSlotsConfig.DEFAULT_PASSIVE_SLOTS;
			this.moverSlots = ExtraSlotsConfig.DEFAULT_MOVER_SLOTS;
			this.identitySlots = ExtraSlotsConfig.DEFAULT_IDENTITY_SLOTS;
			this.maxPassiveSlots = ExtraSlotsConfig.DEFAULT_MAX_PASSIVE_SLOTS;
			this.maxMoverSlots = ExtraSlotsConfig.DEFAULT_MAX_MOVER_SLOTS;
			this.maxIdentitySlots = ExtraSlotsConfig.DEFAULT_MAX_IDENTITY_SLOTS;
			this.rebuildWidgets();
		}).bounds(centerX - 154, this.height - 64, 148, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable(ExtraSlotsConfig.key("mutual_exclusions")), button -> this.minecraft.setScreen(new ExtraSlotsMutualExclusionConfigScreen(this)))
			.bounds(centerX - 100, this.height - 88, 200, 20).build());
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.returnToParent())
			.bounds(centerX + 6, this.height - 64, 148, 20).build());
		
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
			this.save(ExtraSlotsConfig.MAX_PASSIVE_SLOTS, this.maxPassiveSlots);
			this.save(ExtraSlotsConfig.MAX_MOVER_SLOTS, this.maxMoverSlots);
			this.save(ExtraSlotsConfig.MAX_IDENTITY_SLOTS, this.maxIdentitySlots);
			
			if (!ExtraSlotsSkillTreeCompat.isLoaded()) {
				this.save(ExtraSlotsConfig.PASSIVE_SLOTS, this.passiveSlots);
				this.save(ExtraSlotsConfig.MOVER_SLOTS, this.moverSlots);
				this.save(ExtraSlotsConfig.IDENTITY_SLOTS, this.identitySlots);
			}
			
			ExtraSlotsConfig.SPEC.save();
			
			ExtraSkillSlots.applyConfiguredSlots();
			ExtraSlotsClientRuntime.expandKnownPlayers();
			
			this.returnToParent();
		}).bounds(centerX - 100, this.height - 36, 200, 20).build());
	}
	
	private void addSlotControls(int centerX, int y, CountGetter getter, CountSetter setter, int min, int max) {
		this.addRenderableWidget(Button.builder(Component.literal("-"), button -> {
			setter.set(Math.max(min, getter.get() - 1));
			this.rebuildWidgets();
		}).bounds(centerX + 25, y, 24, 20).build());
		
		this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
			setter.set(Math.min(max, getter.get() + 1));
			this.rebuildWidgets();
		}).bounds(centerX + 120, y, 24, 20).build());
	}
	
	private void save(IntValue value, int count) {
		value.set(count);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderDirtBackground(guiGraphics);
		
		int centerX = this.width / 2;
		int titleY = this.titleY();
		guiGraphics.drawCenteredString(this.font, this.title, centerX, titleY, 16777215);

		int startY = this.firstSlotY();
		int maxStartY = ExtraSlotsSkillTreeCompat.isLoaded() ? startY : startY + this.slotGroupGap();
		
		if (ExtraSlotsSkillTreeCompat.isLoaded()) {
			this.drawCenteredWrapped(guiGraphics, Component.translatable(ExtraSlotsConfig.key("skilltree_controls_slots")), centerX, titleY + 22, this.wrappedTextWidth(), 10526880);
		} else {
			this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("passive_slots"), this.passiveSlots, startY);
			this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("mover_slots"), this.moverSlots, startY + 24);
			this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("identity_slots"), this.identitySlots, startY + 48);
		}
		
		this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("max_passive_slots"), this.maxPassiveSlots, maxStartY);
		this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("max_mover_slots"), this.maxMoverSlots, maxStartY + 24);
		this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("max_identity_slots"), this.maxIdentitySlots, maxStartY + 48);
		this.drawCenteredWrapped(guiGraphics, Component.translatable(ExtraSlotsConfig.key("requires_reopen")), centerX, maxStartY + 78, this.width - 32, 10526880);
		
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	private void drawSlotLine(GuiGraphics guiGraphics, String labelKey, int value, int y) {
		int centerX = this.width / 2;
		guiGraphics.fill(centerX - 154, y - 3, centerX + 154, y + 21, ROW_FILL);
		guiGraphics.drawString(this.font, Component.translatable(labelKey), centerX - 144, y + 3, 16777215);
		guiGraphics.drawCenteredString(this.font, Component.literal(String.valueOf(value)), centerX + 84, y + 3, 16777215);
	}

	private int titleY() {
		return 18;
	}

	private int firstSlotY() {
		if (ExtraSlotsSkillTreeCompat.isLoaded()) {
			int noticeLines = this.wrappedLineCount(Component.translatable(ExtraSlotsConfig.key("skilltree_controls_slots")), this.wrappedTextWidth());
			int minimumTop = this.titleY() + 22 + noticeLines * WRAPPED_LINE_HEIGHT + 12;
			int centeredTop = this.height / 2 - 54;
			int maximumTop = this.height - 112;
			return this.clamp(Math.max(centeredTop, minimumTop), minimumTop, maximumTop);
		}

		int minimumTop = this.titleY() + 32;
		int centeredTop = this.height / 2 - 96;
		int maximumTop = this.height - 180;
		return this.clamp(Math.max(centeredTop, minimumTop), minimumTop, maximumTop);
	}

	private int slotGroupGap() {
		return this.height < 300 ? 68 : 80;
	}

	private int wrappedTextWidth() {
		return Math.max(80, this.width - 48);
	}

	private int wrappedLineCount(Component text, int maxWidth) {
		return this.font.split(text, Math.max(80, maxWidth)).size();
	}

	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(value, Math.max(min, max)));
	}

	private void drawCenteredWrapped(GuiGraphics guiGraphics, Component text, int centerX, int y, int maxWidth, int color) {
		List<FormattedCharSequence> lines = this.font.split(text, Math.max(80, maxWidth));
		for (int i = 0; i < lines.size(); i++) {
			FormattedCharSequence line = lines.get(i);
			guiGraphics.drawString(this.font, line, centerX - this.font.width(line) / 2, y + i * WRAPPED_LINE_HEIGHT, color);
		}
	}

	@Override
	public void onClose() {
		this.returnToParent();
	}
	
	private void returnToParent() {
		this.minecraft.setScreen(this.parent == this ? null : this.parent);
	}
	
	private interface CountGetter {
		int get();
	}
	
	private interface CountSetter {
		void set(int value);
	}
}
