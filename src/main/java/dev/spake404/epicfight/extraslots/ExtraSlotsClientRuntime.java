package dev.spake404.epicfight.extraslots;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

@Mod.EventBusSubscriber(modid = EpicFightSkillExtraSlots.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ExtraSlotsClientRuntime {
	private static final String SKILL_EDIT_SCREEN = "yesman.epicfight.client.gui.screen.SkillEditScreen";
	private static final String SLOT_SELECT_SCREEN = "yesman.epicfight.client.gui.screen.SlotSelectScreen";
	private static final String SLOT_SELECT_SCROLL_ARROW = SLOT_SELECT_SCREEN + "$ScrollArrow";
	private static final String SKILL_TREE_SCREEN = "com.yesman.epicskills.client.gui.screen.SkillTreeScreen";
	private static final String SKILL_INFO_SCREEN = "com.yesman.epicskills.client.gui.screen.SkillInfoScreen";
	
	private static Field editSlotButtonsField;
	private static Field editEquipSkillButtonsField;
	private static Field editSelectedSlotButtonField;
	private static Field editMaxScrollField;
	private static Field editScrollField;
	private static Field editUpField;
	private static Field editDownField;
	private static Method editSetScrollVisibilitiesMethod;
	
	private static Field selectContainersField;
	private static Field selectSlotButtonsField;
	private static Field selectMaxScrollField;
	private static Field selectScrollField;
	private static Method selectSetScrollVisibilitiesMethod;
	private static Field infoSkillField;
	private static Field infoNodeField;
	private static Method infoGetActionButtonMethod;
	private static Method infoNodeStateMethod;
	private static Field skillTreeExpConversionButtonField;
	private static Method skillTreeButtonGetWidthMethod;
	
	private ExtraSlotsClientRuntime() {
	}
	
	public static void registerSkillTreeCategoryTextures() {
		if (!ExtraSlotsSkillTreeCompat.isLoaded()) {
			return;
		}
		
		try {
			Class<?> textureClass = Class.forName("com.yesman.epicskills.client.gui.screen.CategorySlotTexture");
			Object manager = textureClass.getField("ENUM_MANAGER").get(null);
			manager.getClass().getMethod("registerEnumCls", String.class, Class.class).invoke(manager, EpicFightSkillExtraSlots.MODID, ExtraSlotUnlockCategoryTextures.class);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
	}
	
	public static void expandKnownPlayers() {
		Minecraft minecraft = Minecraft.getInstance();
		
		if (minecraft.player != null) {
			expandAndClean(EpicFightCapabilities.getPlayerPatch(minecraft.player));
		}
		
		MinecraftServer server = minecraft.getSingleplayerServer();
		
		if (server != null) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				expandAndClean(EpicFightCapabilities.getPlayerPatch(player));
			}
		}
	}
	
	public static void applyRemoteCounts(int passiveSlots, int moverSlots, int identitySlots) {
		ExtraSlotsConfig.PASSIVE_SLOTS.set(Math.max(0, passiveSlots - ExtraSlotsConfig.BASE_PASSIVE_SLOTS));
		ExtraSlotsConfig.MOVER_SLOTS.set(Math.max(0, moverSlots - ExtraSlotsConfig.BASE_MOVER_SLOTS));
		ExtraSlotsConfig.IDENTITY_SLOTS.set(Math.max(0, identitySlots - ExtraSlotsConfig.BASE_IDENTITY_SLOTS));
		ExtraSkillSlots.applyConfiguredSlots();
		expandKnownPlayers();
	}
	
	public static void applySoulStoneCounts(int passiveStones, int moverStones, int identityStones) {
		ExtraSlotsClientSoulStones.set(passiveStones, moverStones, identityStones);
		
		Minecraft minecraft = Minecraft.getInstance();
		
		if (minecraft.screen != null && isScreen(minecraft.screen, SKILL_INFO_SCREEN)) {
			updateSkillInfoUnlockButton(minecraft.screen);
		}
	}
	
	private static void expandAndClean(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return;
		}
		
		ExtraSlotsRuntimeExpander.expand(playerPatch);
		ExtraSlotsRuntimeExpander.clearDisabledSlots(playerPatch.getSkillCapability());
	}
	
	@SubscribeEvent
	public static void onScreenInit(ScreenEvent.Init.Post event) {
		if (isScreen(event.getScreen(), SKILL_EDIT_SCREEN)) {
			hideDisabledSkillEditButtons(event.getScreen(), event);
		} else if (isScreen(event.getScreen(), SLOT_SELECT_SCREEN)) {
			hideDisabledSlotSelectButtons(event.getScreen(), event);
		} else if (isScreen(event.getScreen(), SKILL_TREE_SCREEN)) {
			addSoulStoneMeters(event.getScreen(), event);
		} else if (isScreen(event.getScreen(), SKILL_INFO_SCREEN)) {
			updateSkillInfoUnlockButton(event.getScreen());
		}
	}
	
	@SubscribeEvent
	public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
		if (isScreen(event.getScreen(), SKILL_EDIT_SCREEN)) {
			hideUnlockSkillEditButtons(event.getScreen());
		} else if (isScreen(event.getScreen(), SKILL_INFO_SCREEN)) {
			updateSkillInfoUnlockButton(event.getScreen());
		}
	}
	
	private static void updateSkillInfoUnlockButton(Screen screen) {
		try {
			ensureSkillInfoReflection(screen.getClass());
			
			if (!(infoSkillField.get(screen) instanceof ExtraSlotUnlockSkill skill) || !"UNLOCKABLE".equals(String.valueOf(infoNodeStateMethod.invoke(infoNodeField.get(screen))))) {
				return;
			}
			
			Object actionButton = infoGetActionButtonMethod.invoke(screen);
			
			if (actionButton instanceof Button button) {
				button.active = storedSoulStones(skill.group()) > 0;
			}
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
	}
	
	private static int storedSoulStones(ExtraSlotsSkillTreeCompat.SlotGroup group) {
		return ExtraSlotsClientSoulStones.get(group);
	}
	
	private static boolean isScreen(Screen screen, String className) {
		return className.equals(screen.getClass().getName());
	}
	
	private static void addSoulStoneMeters(Screen screen, ScreenEvent.Init.Post event) {
		if (!ExtraSlotsSkillTreeCompat.isLoaded()) {
			return;
		}
		
		int x = soulStoneStartX(screen);
		int y = 10;
		addSoulStoneMeter(screen, event, new SoulStoneMeter(x, y, ExtraSlotsItems.PASSIVE_SKILLSLOT_SOUL_STONE.get(), ExtraSlotsSkillTreeCompat.SlotGroup.PASSIVE));
		addSoulStoneMeter(screen, event, new SoulStoneMeter(x + 54, y, ExtraSlotsItems.MOVER_SKILLSLOT_SOUL_STONE.get(), ExtraSlotsSkillTreeCompat.SlotGroup.MOVER));
		addSoulStoneMeter(screen, event, new SoulStoneMeter(x + 108, y, ExtraSlotsItems.IDENTITY_SKILLSLOT_SOUL_STONE.get(), ExtraSlotsSkillTreeCompat.SlotGroup.IDENTITY));
	}
	
	private static int soulStoneStartX(Screen screen) {
		int abilityPointsX = abilityPointsMeterX(screen);
		return Math.max(8, abilityPointsX - 162);
	}
	
	private static int abilityPointsMeterX(Screen screen) {
		try {
			if (skillTreeExpConversionButtonField == null) {
				skillTreeExpConversionButtonField = field(screen.getClass(), "expConversionButton");
			}
			
			Object expConversionButton = skillTreeExpConversionButtonField.get(screen);
			
			if (expConversionButton != null) {
				if (skillTreeButtonGetWidthMethod == null) {
					skillTreeButtonGetWidthMethod = expConversionButton.getClass().getMethod("getWidth");
					skillTreeButtonGetWidthMethod.setAccessible(true);
				}
				
				return screen.width - (int)skillTreeButtonGetWidthMethod.invoke(expConversionButton) - 150;
			}
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
		
		return screen.width - 268;
	}
	
	private static void addSoulStoneMeter(Screen screen, ScreenEvent.Init.Post event, SoulStoneMeter meter) {
		event.addListener(meter);
		screen.renderables.add(meter);
	}
	
	@SuppressWarnings("unchecked")
	private static void hideDisabledSkillEditButtons(Screen screen, ScreenEvent.Init.Post event) {
		try {
			ensureSkillEditReflection(screen.getClass());
			
			Map<SkillSlot, GuiEventListener> slotButtons = (Map<SkillSlot, GuiEventListener>)editSlotButtonsField.get(screen);
			int visibleIndex = 0;
			int startY = screen.height / 2 - 82;
			Iterator<Map.Entry<SkillSlot, GuiEventListener>> iterator = slotButtons.entrySet().iterator();
			
			while (iterator.hasNext()) {
				Map.Entry<SkillSlot, GuiEventListener> entry = iterator.next();
				GuiEventListener button = entry.getValue();
				
				if (ExtraSlotUnlockSlots.isUnlockSlot(entry.getKey()) || ExtraSkillSlots.isManagedSlot(entry.getKey()) && !ExtraSkillSlots.isEnabled(entry.getKey())) {
					event.removeListener(button);
					screen.renderables.remove(button);
					iterator.remove();
				} else {
					if (button instanceof AbstractWidget widget) {
						widget.setY(startY + visibleIndex * 18);
					}
					
					visibleIndex++;
				}
			}
			
			hideUnlockSkillEditButtons(screen, event);
			
			Object selectedSlotButton = editSelectedSlotButtonField.get(screen);
			
			if (selectedSlotButton instanceof GuiEventListener listener && !slotButtons.containsValue(listener)) {
				editSelectedSlotButtonField.set(screen, null);
			}
			
			editScrollField.setInt(screen, 0);
			editMaxScrollField.setInt(screen, Math.max(0, visibleIndex - 9));
			
			if (visibleIndex <= 9) {
				removeScrollArrow(screen, event, editUpField);
				removeScrollArrow(screen, event, editDownField);
			}
			
			editSetScrollVisibilitiesMethod.invoke(screen);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
	}
	
	private static void hideUnlockSkillEditButtons(Screen screen) {
		try {
			ensureSkillEditReflection(screen.getClass());
			hideUnlockSkillEditButtons(screen, null);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void hideUnlockSkillEditButtons(Screen screen, ScreenEvent.Init.Post event) throws IllegalAccessException {
		List<GuiEventListener> equipSkillButtons = (List<GuiEventListener>)editEquipSkillButtonsField.get(screen);
		Iterator<GuiEventListener> equipIterator = equipSkillButtons.iterator();
		
		while (equipIterator.hasNext()) {
			GuiEventListener button = equipIterator.next();
			
			if (containsUnlockSkill(button)) {
				if (event != null) {
					event.removeListener(button);
				}
				
				screen.renderables.remove(button);
				screen.children().remove(button);
				equipIterator.remove();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void hideDisabledSlotSelectButtons(Screen screen, ScreenEvent.Init.Post event) {
		try {
			ensureSlotSelectReflection(screen.getClass());
			
			List<SkillContainer> containers = (List<SkillContainer>)selectContainersField.get(screen);
			List<GuiEventListener> slotButtons = (List<GuiEventListener>)selectSlotButtonsField.get(screen);
			List<GuiEventListener> visibleButtons = new ArrayList<>();
			int startX = screen.width / 2 - 84;
			int startY = screen.height / 2 - 40;
			
			for (int index = 0; index < slotButtons.size(); index++) {
				GuiEventListener button = slotButtons.get(index);
				SkillContainer container = containers.get(index);
				
				if (ExtraSlotUnlockSlots.isUnlockSlot(container.getSlot()) || ExtraSkillSlots.isManagedSlot(container.getSlot()) && !ExtraSkillSlots.isEnabled(container.getSlot())) {
					event.removeListener(button);
					screen.renderables.remove(button);
				} else {
					if (button instanceof AbstractWidget widget) {
						int visibleIndex = visibleButtons.size();
						widget.setX(startX + (visibleIndex % 3) * 60);
						widget.setY(startY + (visibleIndex / 3) * 60);
					}
					
					visibleButtons.add(button);
				}
			}
			
			if (visibleButtons.size() != slotButtons.size()) {
				slotButtons.clear();
				slotButtons.addAll(visibleButtons);
			}
			
			selectScrollField.setInt(screen, 0);
			selectMaxScrollField.setInt(screen, Math.max(0, ((visibleButtons.size() + 2) / 3) - 2));
			
			if (visibleButtons.size() <= 6) {
				removeSlotSelectScrollArrows(screen, event);
			}
			
			selectSetScrollVisibilitiesMethod.invoke(screen);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
	}
	
	private static void removeScrollArrow(Screen screen, ScreenEvent.Init.Post event, Field field) throws IllegalAccessException {
		Object arrow = field.get(screen);
		
		if (arrow instanceof GuiEventListener listener) {
			event.removeListener(listener);
			screen.renderables.remove(arrow);
			field.set(screen, null);
		}
	}
	
	private static void removeSlotSelectScrollArrows(Screen screen, ScreenEvent.Init.Post event) {
		List<GuiEventListener> listeners = new ArrayList<>(event.getListenersList());
		
		for (GuiEventListener listener : listeners) {
			if (SLOT_SELECT_SCROLL_ARROW.equals(listener.getClass().getName())) {
				event.removeListener(listener);
				screen.renderables.remove(listener);
			}
		}
	}
	
	private static void ensureSkillEditReflection(Class<?> screenClass) throws NoSuchFieldException, NoSuchMethodException {
		if (editSlotButtonsField != null) {
			return;
		}
		
		editSlotButtonsField = field(screenClass, "slotButtons");
		editEquipSkillButtonsField = field(screenClass, "equipSkillButtons");
		editSelectedSlotButtonField = field(screenClass, "selectedSlotButton");
		editMaxScrollField = field(screenClass, "maxScroll");
		editScrollField = field(screenClass, "scroll");
		editUpField = field(screenClass, "up");
		editDownField = field(screenClass, "down");
		
		editSetScrollVisibilitiesMethod = screenClass.getDeclaredMethod("setScrollVisibilities");
		editSetScrollVisibilitiesMethod.setAccessible(true);
	}
	
	private static void ensureSlotSelectReflection(Class<?> screenClass) throws NoSuchFieldException, NoSuchMethodException {
		if (selectSlotButtonsField != null) {
			return;
		}
		
		selectContainersField = field(screenClass, "containers");
		selectSlotButtonsField = field(screenClass, "slotButtons");
		selectMaxScrollField = field(screenClass, "maxScroll");
		selectScrollField = field(screenClass, "scroll");
		
		selectSetScrollVisibilitiesMethod = screenClass.getDeclaredMethod("setScrollVisibilities");
		selectSetScrollVisibilitiesMethod.setAccessible(true);
	}
	
	private static void ensureSkillInfoReflection(Class<?> screenClass) throws NoSuchFieldException, NoSuchMethodException {
		if (infoSkillField != null) {
			return;
		}
		
		infoSkillField = fieldRecursive(screenClass, "skill");
		infoNodeField = field(screenClass, "node");
		infoGetActionButtonMethod = screenClass.getMethod("getActionButton");
		infoGetActionButtonMethod.setAccessible(true);
		infoNodeStateMethod = infoNodeField.getType().getMethod("nodeState");
		infoNodeStateMethod.setAccessible(true);
	}
	
	private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}
	
	private static Field fieldRecursive(Class<?> owner, String name) throws NoSuchFieldException {
		Class<?> type = owner;
		
		while (type != null && type != Object.class) {
			try {
				return field(type, name);
			} catch (NoSuchFieldException ignored) {
				type = type.getSuperclass();
			}
		}
		
		throw new NoSuchFieldException(name);
	}
	
	private static boolean containsUnlockSkill(Object target) {
		if (target == null) {
			return false;
		}
		
		Class<?> type = target.getClass();
		
		while (type != null && type != Object.class) {
			for (Field field : type.getDeclaredFields()) {
				if (canHoldUnlockSkill(field)) {
					try {
						field.setAccessible(true);
						
						if (field.get(target) instanceof ExtraSlotUnlockSkill) {
							return true;
						}
					} catch (ReflectiveOperationException | RuntimeException ignored) {
					}
				}
			}
			
			type = type.getSuperclass();
		}
		
		return false;
	}
	
	private static boolean canHoldUnlockSkill(Field field) {
		Class<?> fieldType = field.getType();
		return fieldType.isAssignableFrom(ExtraSlotUnlockSkill.class) || ExtraSlotUnlockSkill.class.isAssignableFrom(fieldType);
	}
	
	private static final class SoulStoneMeter extends AbstractWidget {
		private final Item item;
		private final ExtraSlotsSkillTreeCompat.SlotGroup group;
		
		private SoulStoneMeter(int x, int y, Item item, ExtraSlotsSkillTreeCompat.SlotGroup group) {
			super(x, y, 50, 18, Component.empty());
			this.item = item;
			this.group = group;
			setTooltip(Tooltip.create(this.item.getDescription()));
		}
		
		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			Minecraft minecraft = Minecraft.getInstance();
			String count = String.valueOf(storedSoulStones(this.group));
			this.width = minecraft.font.width(count) + 24;
			guiGraphics.renderItem(new ItemStack(this.item), getX(), getY());
			guiGraphics.drawString(minecraft.font, count, getX() + 24, getY() + 5, 16777215);
		}
		
		@Override
		protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
	
}
