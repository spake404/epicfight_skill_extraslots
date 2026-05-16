package dev.spake404.epicfight.extraslots;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
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
	private static Field editSlotButtonsField;
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
	
	private ExtraSlotsClientRuntime() {
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
		ExtraSlotsConfig.PASSIVE_SLOTS.set(passiveSlots);
		ExtraSlotsConfig.MOVER_SLOTS.set(moverSlots);
		ExtraSlotsConfig.IDENTITY_SLOTS.set(identitySlots);
		ExtraSkillSlots.applyConfiguredSlots();
		expandKnownPlayers();
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
		if ("yesman.epicfight.client.gui.screen.SkillEditScreen".equals(event.getScreen().getClass().getName())) {
			hideDisabledSkillEditButtons(event.getScreen(), event);
		} else if ("yesman.epicfight.client.gui.screen.SlotSelectScreen".equals(event.getScreen().getClass().getName())) {
			hideDisabledSlotSelectButtons(event.getScreen(), event);
		}
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
				
				if (ExtraSkillSlots.isManagedSlot(entry.getKey()) && !ExtraSkillSlots.isEnabled(entry.getKey())) {
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
				
				if (ExtraSkillSlots.isManagedSlot(container.getSlot()) && !ExtraSkillSlots.isEnabled(container.getSlot())) {
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
			if ("yesman.epicfight.client.gui.screen.SlotSelectScreen$ScrollArrow".equals(listener.getClass().getName())) {
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
	
	private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}
}
