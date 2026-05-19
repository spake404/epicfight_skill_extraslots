package dev.spake404.epicfight.extraslots;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.skill.SkillCategory;

public final class ExtraSlotUnlockCategories implements SkillCategory {
	private static final Map<String, ExtraSlotUnlockCategories> REGISTERED = new LinkedHashMap<>();
	private static ExtraSlotUnlockCategories[] values;
	
	private final String name;
	private final ExtraSlotsSkillTreeCompat.SlotGroup group;
	private final int id;
	
	private ExtraSlotUnlockCategories(String name, ExtraSlotsSkillTreeCompat.SlotGroup group) {
		this.name = name;
		this.group = group;
		this.id = SkillCategory.ENUM_MANAGER.assign(this);
	}
	
	static ExtraSlotUnlockCategories get(ExtraSlotsSkillTreeCompat.SlotGroup group, int slotIndex) {
		ensureCategories();
		return REGISTERED.get(name(group, slotIndex));
	}
	
	public static ExtraSlotUnlockCategories[] values() {
		ensureCategories();
		return values.clone();
	}
	
	private static synchronized void ensureCategories() {
		for (ExtraSlotsSkillTreeCompat.SlotGroup group : ExtraSlotsSkillTreeCompat.SlotGroup.values()) {
			for (int slotIndex = group.baseSlots() + 1; slotIndex <= group.registeredMaxSlots(); slotIndex++) {
				String name = name(group, slotIndex);
				REGISTERED.computeIfAbsent(name, ignored -> new ExtraSlotUnlockCategories(name, group));
			}
		}
		
		values = REGISTERED.values().toArray(ExtraSlotUnlockCategories[]::new);
	}
	
	private static String name(ExtraSlotsSkillTreeCompat.SlotGroup group, int slotIndex) {
		return "EXTRASLOT_" + group.name() + '_' + slotIndex + "_CATEGORY";
	}
	
	@Override
	public boolean shouldSave() {
		return true;
	}
	
	@Override
	public boolean shouldSynchronize() {
		return true;
	}
	
	@Override
	public boolean learnable() {
		return true;
	}
	
	@Override
	public ResourceLocation bookIcon() {
		return SkillCategory.DEFAULT_BOOK_ICON;
	}
	
	ExtraSlotsSkillTreeCompat.SlotGroup group() {
		return this.group;
	}
	
	@Override
	public int universalOrdinal() {
		return this.id;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
