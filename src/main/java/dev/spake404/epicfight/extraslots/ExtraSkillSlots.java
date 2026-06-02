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
			ensureSlots(startupRegisteredPassiveSlots(), startupRegisteredMoverSlots(), startupRegisteredIdentitySlots());
		}

		return values.clone();
	}

	public static int applyConfiguredSlots() {
		int previousCount = REGISTERED.size();
		ensureSlots(registeredPassiveSlots(), registeredMoverSlots(), registeredIdentitySlots());
		return REGISTERED.size() - previousCount;
	}

	public static boolean isManagedSlot(SkillSlot slot) {
		String slotName = normalizedName(slot);

		return isExtraSlot(slotName, "PASSIVE", ExtraSlotsConfig.BASE_PASSIVE_SLOTS, ExtraSlotsConfig.BASE_PASSIVE_SLOTS + ExtraSlotsConfig.HARD_MAX_PASSIVE_SLOTS)
			|| isExtraSlot(slotName, "MOVER", ExtraSlotsConfig.BASE_MOVER_SLOTS, ExtraSlotsConfig.BASE_MOVER_SLOTS + ExtraSlotsConfig.HARD_MAX_MOVER_SLOTS)
			|| isExtraSlot(slotName, "IDENTITY", ExtraSlotsConfig.BASE_IDENTITY_SLOTS, ExtraSlotsConfig.BASE_IDENTITY_SLOTS + ExtraSlotsConfig.HARD_MAX_IDENTITY_SLOTS);
	}

	public static boolean isEnabled(SkillSlot slot) {
		return isEnabled(slot, ExtraSlotCounts.configured());
	}

	public static boolean isEnabled(SkillSlot slot, ExtraSlotCounts counts) {
		String slotName = normalizedName(slot);

		if (!isManagedSlot(slot)) {
			return true;
		}

		if (slotName.startsWith("PASSIVE")) {
			return slotIndex(slotName, "PASSIVE") <= counts.passiveSlots();
		} else if (slotName.startsWith("MOVER")) {
			return slotIndex(slotName, "MOVER") <= counts.moverSlots();
		} else if (slotName.startsWith("IDENTITY")) {
			return slotIndex(slotName, "IDENTITY") <= counts.identitySlots();
		}

		return true;
	}

	private static int startupRegisteredPassiveSlots() {
		return ExtraSlotsSkillTreeCompat.isLoaded() ? ExtraSlotsConfig.BASE_PASSIVE_SLOTS + ExtraSlotsConfig.HARD_MAX_PASSIVE_SLOTS : ExtraSlotsConfig.startupPassiveSlots();
	}

	private static int startupRegisteredMoverSlots() {
		return ExtraSlotsSkillTreeCompat.isLoaded() ? ExtraSlotsConfig.BASE_MOVER_SLOTS + ExtraSlotsConfig.HARD_MAX_MOVER_SLOTS : ExtraSlotsConfig.startupMoverSlots();
	}

	private static int startupRegisteredIdentitySlots() {
		return ExtraSlotsSkillTreeCompat.isLoaded() ? ExtraSlotsConfig.BASE_IDENTITY_SLOTS + ExtraSlotsConfig.HARD_MAX_IDENTITY_SLOTS : ExtraSlotsConfig.startupIdentitySlots();
	}

	private static int registeredPassiveSlots() {
		return ExtraSlotsSkillTreeCompat.isLoaded() ? ExtraSlotsConfig.BASE_PASSIVE_SLOTS + ExtraSlotsConfig.HARD_MAX_PASSIVE_SLOTS : ExtraSlotsConfig.passiveSlots();
	}

	private static int registeredMoverSlots() {
		return ExtraSlotsSkillTreeCompat.isLoaded() ? ExtraSlotsConfig.BASE_MOVER_SLOTS + ExtraSlotsConfig.HARD_MAX_MOVER_SLOTS : ExtraSlotsConfig.moverSlots();
	}

	private static int registeredIdentitySlots() {
		return ExtraSlotsSkillTreeCompat.isLoaded() ? ExtraSlotsConfig.BASE_IDENTITY_SLOTS + ExtraSlotsConfig.HARD_MAX_IDENTITY_SLOTS : ExtraSlotsConfig.identitySlots();
	}

	private static synchronized void ensureSlots(int passiveSlots, int moverSlots, int identitySlots) {
		addSlots("PASSIVE", SkillCategories.PASSIVE, ExtraSlotsConfig.BASE_PASSIVE_SLOTS, passiveSlots);
		addSlots("MOVER", SkillCategories.MOVER, ExtraSlotsConfig.BASE_MOVER_SLOTS, moverSlots);
		addSlots("IDENTITY", SkillCategories.IDENTITY, ExtraSlotsConfig.BASE_IDENTITY_SLOTS, identitySlots);
		values = REGISTERED.toArray(ExtraSkillSlots[]::new);
	}

	private static void addSlots(String prefix, SkillCategory category, int baseSlots, int totalSlots) {
		for (int index = baseSlots + 1; index <= totalSlots; index++) {
			String name = prefix + index;

			if (!isRegistered(name)) {
				REGISTERED.add(new ExtraSkillSlots(name, category));
			}
		}
	}

	private static boolean isRegistered(String name) {
		return SkillSlot.ENUM_MANAGER.get(name) != null;
	}

	private static String normalizedName(SkillSlot slot) {
		if (slot == null) {
			return "";
		}

		String name = slot instanceof ExtraSkillSlots extraSlot ? extraSlot.name : slot.toString();
		return name.toUpperCase(Locale.ROOT);
	}

	private static boolean isExtraSlot(String slotName, String prefix, int baseSlots, int maxSlots) {
		int index = slotIndex(slotName, prefix);
		return index > baseSlots && index <= maxSlots;
	}

	private static int slotIndex(String slotName, String prefix) {
		if (!slotName.startsWith(prefix)) {
			return -1;
		}

		try {
			return Integer.parseInt(slotName.substring(prefix.length()));
		} catch (NumberFormatException ignored) {
			return -1;
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
