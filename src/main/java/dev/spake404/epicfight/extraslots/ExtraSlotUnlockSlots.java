package dev.spake404.epicfight.extraslots;

import java.util.LinkedHashMap;
import java.util.Map;

import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;

public final class ExtraSlotUnlockSlots implements SkillSlot {
	private static final Map<String, ExtraSlotUnlockSlots> REGISTERED = new LinkedHashMap<>();
	private static ExtraSlotUnlockSlots[] values;
	
	private final String name;
	private final SkillCategory category;
	private final int id;
	
	private ExtraSlotUnlockSlots(String name, SkillCategory category) {
		this.name = name;
		this.category = category;
		this.id = SkillSlot.ENUM_MANAGER.assign(this);
	}
	
	static ExtraSlotUnlockSlots get(ExtraSlotsSkillTreeCompat.SlotGroup group, int slotIndex) {
		ensureSlots();
		return REGISTERED.get(name(group, slotIndex));
	}
	
	public static boolean isUnlockSlot(SkillSlot slot) {
		return slot instanceof ExtraSlotUnlockSlots;
	}
	
	public static ExtraSlotUnlockSlots[] values() {
		ensureSlots();
		return values.clone();
	}
	
	private static synchronized void ensureSlots() {
		for (ExtraSlotsSkillTreeCompat.SlotGroup group : ExtraSlotsSkillTreeCompat.SlotGroup.values()) {
			for (int slotIndex = group.baseSlots() + 1; slotIndex <= group.registeredMaxSlots(); slotIndex++) {
				String name = name(group, slotIndex);
				int index = slotIndex;
				REGISTERED.computeIfAbsent(name, ignored -> new ExtraSlotUnlockSlots(name, ExtraSlotUnlockCategories.get(group, index)));
			}
		}
		
		values = REGISTERED.values().toArray(ExtraSlotUnlockSlots[]::new);
	}
	
	private static String name(ExtraSlotsSkillTreeCompat.SlotGroup group, int slotIndex) {
		return "EXTRASLOT_" + group.name() + '_' + slotIndex + "_SLOT";
	}
	
	@Override
	public SkillCategory category() {
		return this.category;
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
