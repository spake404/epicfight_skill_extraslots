package dev.spake404.epicfight.extraslots;

import java.util.ArrayList;
import java.util.List;

import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;

public final class ExtraSkillSlots implements SkillSlot {
	private static ExtraSkillSlots[] values;
	
	private final String name;
	private final SkillCategory category;
	private final int id;
	
	private ExtraSkillSlots(String name, SkillCategory category) {
		this.name = name;
		this.category = category;
		this.id = SkillSlot.ENUM_MANAGER.assign(this);
	}
	
	public static ExtraSkillSlots[] values() {
		if (values == null) {
			values = createConfiguredSlots();
		}
		
		return values.clone();
	}
	
	private static ExtraSkillSlots[] createConfiguredSlots() {
		List<ExtraSkillSlots> slots = new ArrayList<>();
		addSlots(slots, "PASSIVE", SkillCategories.PASSIVE, ExtraSlotsConfig.BASE_PASSIVE_SLOTS + 1, ExtraSlotsConfig.startupPassiveSlots());
		addSlots(slots, "MOVER", SkillCategories.MOVER, ExtraSlotsConfig.BASE_MOVER_SLOTS + 1, ExtraSlotsConfig.startupMoverSlots());
		addSlots(slots, "IDENTITY", SkillCategories.IDENTITY, ExtraSlotsConfig.BASE_IDENTITY_SLOTS + 1, ExtraSlotsConfig.startupIdentitySlots());
		return slots.toArray(ExtraSkillSlots[]::new);
	}
	
	private static void addSlots(List<ExtraSkillSlots> slots, String prefix, SkillCategory category, int firstExtraIndex, int totalSlots) {
		for (int index = firstExtraIndex; index <= totalSlots; index++) {
			slots.add(new ExtraSkillSlots(prefix + index, category));
		}
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
