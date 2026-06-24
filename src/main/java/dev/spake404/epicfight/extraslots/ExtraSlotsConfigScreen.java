package dev.spake404.epicfight.extraslots;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public class ExtraSlotsConfigScreen extends Screen {
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
		int startY = this.height / 2 - 96;
		
		int maxStartY = ExtraSlotsSkillTreeCompat.isLoaded() ? startY : startY + 80;
		
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
			.bounds(centerX + 6, this.height - 88, 148, 20).build());
		
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
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 16777215);
		
		int centerX = this.width / 2;
		int startY = this.height / 2 - 92;
		int maxStartY = ExtraSlotsSkillTreeCompat.isLoaded() ? startY : startY + 80;
		
		if (ExtraSlotsSkillTreeCompat.isLoaded()) {
			guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("skilltree_controls_slots")), centerX, startY - 22, 10526880);
		} else {
			this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("passive_slots"), this.passiveSlots, startY);
			this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("mover_slots"), this.moverSlots, startY + 24);
			this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("identity_slots"), this.identitySlots, startY + 48);
		}
		
		this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("max_passive_slots"), this.maxPassiveSlots, maxStartY);
		this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("max_mover_slots"), this.maxMoverSlots, maxStartY + 24);
		this.drawSlotLine(guiGraphics, ExtraSlotsConfig.key("max_identity_slots"), this.maxIdentitySlots, maxStartY + 48);
		guiGraphics.drawCenteredString(this.font, Component.translatable(ExtraSlotsConfig.key("requires_reopen")), centerX, maxStartY + 78, 10526880);
		
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	private void drawSlotLine(GuiGraphics guiGraphics, String labelKey, int value, int y) {
		guiGraphics.drawString(this.font, Component.translatable(labelKey), this.width / 2 - 144, y + 2, 16777215);
		guiGraphics.drawCenteredString(this.font, Component.literal(String.valueOf(value)), this.width / 2 + 84, y + 2, 16777215);
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
