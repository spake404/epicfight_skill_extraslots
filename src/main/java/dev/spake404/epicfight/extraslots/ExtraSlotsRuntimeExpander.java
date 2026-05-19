package dev.spake404.epicfight.extraslots;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

public final class ExtraSlotsRuntimeExpander {
	private static Field skillContainersField;
	private static Field containersByCategoryField;
	private static Method categoryPutMethod;
	
	private ExtraSlotsRuntimeExpander() {
	}
	
	public static void expand(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return;
		}
		
		expand(playerPatch.getSkillCapability(), playerPatch);
	}
	
	public static void expandAndClean(PlayerPatch<?> playerPatch) {
		if (playerPatch == null) {
			return;
		}
		
		expand(playerPatch);
		clearDisabledSlots(playerPatch.getSkillCapability());
	}
	
	public static void clearDisabledSlots(CapabilitySkill skills) {
		clearDisabledSlots(skills, ExtraSlotCounts.configured());
	}
	
	public static void clearDisabledSlots(CapabilitySkill skills, ExtraSlotCounts counts) {
		if (skills == null) {
			return;
		}
		
		skills.listSkillContainers()
			.filter(container -> container != null && ExtraSkillSlots.isManagedSlot(container.getSlot()) && !ExtraSkillSlots.isEnabled(container.getSlot(), counts))
			.forEach(container -> {
				if (!container.isEmpty()) {
					container.setSkill(null);
				}
			});
	}
	
	public static void clearUnlockMarkerSkills(CapabilitySkill skills) {
		if (skills == null) {
			return;
		}
		
		skills.listSkillContainers()
			.filter(container -> container != null && container.getSkill() instanceof ExtraSlotUnlockSkill && !ExtraSlotUnlockSlots.isUnlockSlot(container.getSlot()))
			.forEach(container -> container.setSkill(null));
	}
	
	public static void moveUnlockMarkerSkillsToHiddenSlots(CapabilitySkill skills) {
		moveUnlockMarkerSkillsToHiddenSlots(skills, null);
	}
	
	public static void moveUnlockMarkerSkillsToHiddenSlots(CapabilitySkill skills, ServerPlayer player) {
		if (skills == null) {
			return;
		}
		
		skills.listSkillContainers()
			.filter(container -> container != null && container.getSkill() instanceof ExtraSlotUnlockSkill && !ExtraSlotUnlockSlots.isUnlockSlot(container.getSlot()))
			.forEach(container -> {
				ExtraSlotUnlockSkill skill = (ExtraSlotUnlockSkill)container.getSkill();
				SkillContainer target = skills.getSkillContainerFor(ExtraSlotUnlockSlots.get(skill.group(), skill.slotIndex()));
				
				container.setSkill(null);
				syncSkillContainer(container, player);
				
				if (target != null && target.getSkill() != skill) {
					target.setSkill(skill);
					syncSkillContainer(target, player);
				}
			});
	}
	
	public static void expand(CapabilitySkill skills, PlayerPatch<?> playerPatch) {
		if (skills == null || playerPatch == null) {
			return;
		}
		
		try {
			ensureReflection();
			
			Collection<SkillSlot> slots = SkillSlot.ENUM_MANAGER.universalValues();
			SkillContainer[] current = (SkillContainer[])skillContainersField.get(skills);
			int requiredLength = requiredLength(slots);
			
			if (current.length >= requiredLength && hasAllContainers(current, slots)) {
				return;
			}
			
			SkillContainer[] expanded = Arrays.copyOf(current, requiredLength);
			Object containersByCategory = containersByCategoryField.get(skills);
			
			for (SkillSlot slot : slots) {
				int ordinal = slot.universalOrdinal();
				
				if (expanded[ordinal] == null) {
					SkillContainer container = new SkillContainer(playerPatch, slot);
					expanded[ordinal] = container;
					categoryPutMethod.invoke(containersByCategory, slot.category(), container);
				}
			}
			
			skillContainersField.set(skills, expanded);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
		}
	}
	
	private static void ensureReflection() throws NoSuchFieldException, NoSuchMethodException, ClassNotFoundException {
		if (skillContainersField != null) {
			return;
		}
		
		skillContainersField = CapabilitySkill.class.getField("skillContainers");
		skillContainersField.setAccessible(true);
		
		containersByCategoryField = CapabilitySkill.class.getDeclaredField("containersByCategory");
		containersByCategoryField.setAccessible(true);
		
		categoryPutMethod = Class.forName("com.google.common.collect.HashMultimap").getMethod("put", Object.class, Object.class);
	}
	
	private static int requiredLength(Collection<SkillSlot> slots) {
		int maxOrdinal = -1;
		
		for (SkillSlot slot : slots) {
			maxOrdinal = Math.max(maxOrdinal, slot.universalOrdinal());
		}
		
		return maxOrdinal + 1;
	}
	
	private static boolean hasAllContainers(SkillContainer[] containers, Collection<SkillSlot> slots) {
		for (SkillSlot slot : slots) {
			int ordinal = slot.universalOrdinal();
			
			if (ordinal >= containers.length || containers[ordinal] == null) {
				return false;
			}
		}
		
		return true;
	}
	
	private static void syncSkillContainer(SkillContainer container, ServerPlayer player) {
		if (player == null) {
			return;
		}
		
		EpicFightNetworkManager.sendToPlayer(container.createSyncPacketToLocalPlayer(), player);
		EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(container.createSyncPacketToRemotePlayer(), player);
	}
}
