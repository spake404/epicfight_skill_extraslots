package dev.spake404.epicfight.extraslots;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;

public final class ExtraSkillSlots implements SkillSlot {
	private static final List<ExtraSkillSlots> REGISTERED = new ArrayList<>();
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
			ensureSlots(ExtraSlotsConfig.startupPassiveSlots(), ExtraSlotsConfig.startupMoverSlots(), ExtraSlotsConfig.startupIdentitySlots());
		}
		
		return values.clone();
	}
	
	public static int applyConfiguredSlots() {
		int previousCount = REGISTERED.size();
		ensureSlots(ExtraSlotsConfig.passiveSlots(), ExtraSlotsConfig.moverSlots(), ExtraSlotsConfig.identitySlots());
		return REGISTERED.size() - previousCount;
	}
	
	public static boolean isManagedSlot(SkillSlot slot) {
		return slot instanceof ExtraSkillSlots;
	}
	
	public static boolean isEnabled(SkillSlot slot) {
		if (!(slot instanceof ExtraSkillSlots extraSlot)) {
			return true;
		}
		
		String slotName = extraSlot.name.toUpperCase(Locale.ROOT);
		
		if (slotName.startsWith("PASSIVE")) {
			return slotIndex(slotName, "PASSIVE") <= ExtraSlotsConfig.passiveSlots();
		} else if (slotName.startsWith("MOVER")) {
			return slotIndex(slotName, "MOVER") <= ExtraSlotsConfig.moverSlots();
		} else if (slotName.startsWith("IDENTITY")) {
			return slotIndex(slotName, "IDENTITY") <= ExtraSlotsConfig.identitySlots();
		}
		
		return true;
	}
	
	private static synchronized void ensureSlots(int passiveSlots, int moverSlots, int identitySlots) {
		addSlots("PASSIVE", SkillCategories.PASSIVE, ExtraSlotsConfig.BASE_PASSIVE_SLOTS, passiveSlots);
		addSlots("MOVER", SkillCategories.MOVER, ExtraSlotsConfig.BASE_MOVER_SLOTS, moverSlots);
		addSlots("IDENTITY", SkillCategories.IDENTITY, ExtraSlotsConfig.BASE_IDENTITY_SLOTS, identitySlots);
		values = REGISTERED.toArray(ExtraSkillSlots[]::new);
	}
	
	private static void addSlots(String prefix, SkillCategory category, int baseSlots, int totalSlots) {
		int firstExtraIndex = baseSlots + countRegistered(category) + 1;
		
		for (int index = firstExtraIndex; index <= totalSlots; index++) {
			REGISTERED.add(new ExtraSkillSlots(prefix + index, category));
		}
	}
	
	private static int countRegistered(SkillCategory category) {
		int count = 0;
		
		for (ExtraSkillSlots slot : REGISTERED) {
			if (slot.category == category) {
				count++;
			}
		}
		
		return count;
	}
	
	private static int slotIndex(String slotName, String prefix) {
		try {
			return Integer.parseInt(slotName.substring(prefix.length()));
		} catch (NumberFormatException ignored) {
			return Integer.MAX_VALUE;
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
