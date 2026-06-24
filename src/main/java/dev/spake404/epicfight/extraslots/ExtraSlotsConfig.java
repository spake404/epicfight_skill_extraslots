package dev.spake404.epicfight.extraslots;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;

public final class ExtraSlotsConfig {
	private static final Pattern CONFIG_VALUE = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*=\\s*(-?\\d+)\\s*(?:#.*)?$");
	
	public static final int BASE_PASSIVE_SLOTS = 3;
	public static final int BASE_MOVER_SLOTS = 1;
	public static final int BASE_IDENTITY_SLOTS = 1;
	public static final int MIN_PASSIVE_SLOTS = 0;
	public static final int MIN_MOVER_SLOTS = 0;
	public static final int MIN_IDENTITY_SLOTS = 0;
	public static final int DEFAULT_PASSIVE_SLOTS = 0;
	public static final int DEFAULT_MOVER_SLOTS = 0;
	public static final int DEFAULT_IDENTITY_SLOTS = 0;
	public static final int DEFAULT_MAX_PASSIVE_SLOTS = 8;
	public static final int DEFAULT_MAX_MOVER_SLOTS = 8;
	public static final int DEFAULT_MAX_IDENTITY_SLOTS = 8;
	public static final int HARD_MAX_PASSIVE_SLOTS = 61;
	public static final int HARD_MAX_MOVER_SLOTS = 63;
	public static final int HARD_MAX_IDENTITY_SLOTS = 63;
	
	private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	
	public static final ForgeConfigSpec.IntValue MAX_PASSIVE_SLOTS;
	public static final ForgeConfigSpec.IntValue MAX_MOVER_SLOTS;
	public static final ForgeConfigSpec.IntValue MAX_IDENTITY_SLOTS;
	public static final ForgeConfigSpec.IntValue PASSIVE_SLOTS;
	public static final ForgeConfigSpec.IntValue MOVER_SLOTS;
	public static final ForgeConfigSpec.IntValue IDENTITY_SLOTS;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MUTUAL_EXCLUSION_GROUPS;
	public static final ForgeConfigSpec SPEC;
	
	static {
		BUILDER.push("skill_slots");
		MAX_PASSIVE_SLOTS = BUILDER.comment("Maximum extra passive skill slots after re-entering the world, not including Epic Fight's built-in passive slots.")
			.defineInRange("max_extra_passive_slots", DEFAULT_MAX_PASSIVE_SLOTS, MIN_PASSIVE_SLOTS, HARD_MAX_PASSIVE_SLOTS);
		MAX_MOVER_SLOTS = BUILDER.comment("Maximum extra mover skill slots after re-entering the world, not including Epic Fight's built-in mover slot.")
			.defineInRange("max_extra_mover_slots", DEFAULT_MAX_MOVER_SLOTS, MIN_MOVER_SLOTS, HARD_MAX_MOVER_SLOTS);
		MAX_IDENTITY_SLOTS = BUILDER.comment("Maximum extra identity skill slots after re-entering the world, not including Epic Fight's built-in identity slot.")
			.defineInRange("max_extra_identity_slots", DEFAULT_MAX_IDENTITY_SLOTS, MIN_IDENTITY_SLOTS, HARD_MAX_IDENTITY_SLOTS);
		PASSIVE_SLOTS = BUILDER.comment("Extra passive skill slots after the next game restart, not including Epic Fight's built-in passive slots.")
			.defineInRange("extra_passive_slots", DEFAULT_PASSIVE_SLOTS, MIN_PASSIVE_SLOTS, HARD_MAX_PASSIVE_SLOTS);
		MOVER_SLOTS = BUILDER.comment("Extra mover skill slots after the next game restart, not including Epic Fight's built-in mover slot.")
			.defineInRange("extra_mover_slots", DEFAULT_MOVER_SLOTS, MIN_MOVER_SLOTS, HARD_MAX_MOVER_SLOTS);
		IDENTITY_SLOTS = BUILDER.comment("Extra identity skill slots after the next game restart, not including Epic Fight's built-in identity slot.")
			.defineInRange("extra_identity_slots", DEFAULT_IDENTITY_SLOTS, MIN_IDENTITY_SLOTS, HARD_MAX_IDENTITY_SLOTS);
		BUILDER.pop();

		BUILDER.push("mutual_exclusion");
		MUTUAL_EXCLUSION_GROUPS = BUILDER.comment(
				"Comma-separated skill ids. Only one skill in each group can be equipped at the same time.",
				"Example: \"epicfight:roll,epicfight:step\""
			)
			.defineList("mutual_exclusion_groups", List.of(), value -> value instanceof String);
		BUILDER.pop();
		
		SPEC = BUILDER.build();
	}
	
	private ExtraSlotsConfig() {
	}
	
	public static int getConfiguredCount(SkillCategory category) {
		if (category == SkillCategories.PASSIVE) {
			return passiveSlots();
		} else if (category == SkillCategories.MOVER) {
			return moverSlots();
		} else if (category == SkillCategories.IDENTITY) {
			return identitySlots();
		}
		
		return 0;
	}
	
	public static int passiveSlots() {
		return BASE_PASSIVE_SLOTS + extraPassiveSlots();
	}
	
	public static int moverSlots() {
		return BASE_MOVER_SLOTS + extraMoverSlots();
	}
	
	public static int identitySlots() {
		return BASE_IDENTITY_SLOTS + extraIdentitySlots();
	}
	
	public static int maxPassiveSlots() {
		return BASE_PASSIVE_SLOTS + maxExtraPassiveSlots();
	}
	
	public static int maxMoverSlots() {
		return BASE_MOVER_SLOTS + maxExtraMoverSlots();
	}
	
	public static int maxIdentitySlots() {
		return BASE_IDENTITY_SLOTS + maxExtraIdentitySlots();
	}
	
	public static int extraPassiveSlots() {
		return clamp(PASSIVE_SLOTS.get(), MIN_PASSIVE_SLOTS, maxExtraPassiveSlots());
	}
	
	public static int extraMoverSlots() {
		return clamp(MOVER_SLOTS.get(), MIN_MOVER_SLOTS, maxExtraMoverSlots());
	}
	
	public static int extraIdentitySlots() {
		return clamp(IDENTITY_SLOTS.get(), MIN_IDENTITY_SLOTS, maxExtraIdentitySlots());
	}
	
	public static int maxExtraPassiveSlots() {
		return clamp(MAX_PASSIVE_SLOTS.get(), MIN_PASSIVE_SLOTS, HARD_MAX_PASSIVE_SLOTS);
	}
	
	public static int maxExtraMoverSlots() {
		return clamp(MAX_MOVER_SLOTS.get(), MIN_MOVER_SLOTS, HARD_MAX_MOVER_SLOTS);
	}
	
	public static int maxExtraIdentitySlots() {
		return clamp(MAX_IDENTITY_SLOTS.get(), MIN_IDENTITY_SLOTS, HARD_MAX_IDENTITY_SLOTS);
	}
	
	public static int startupPassiveSlots() {
		int max = startupMaxExtraPassiveSlots();
		return BASE_PASSIVE_SLOTS + readStartupSlotCount("extra_passive_slots", DEFAULT_PASSIVE_SLOTS, MIN_PASSIVE_SLOTS, max);
	}
	
	public static int startupMoverSlots() {
		int max = startupMaxExtraMoverSlots();
		return BASE_MOVER_SLOTS + readStartupSlotCount("extra_mover_slots", DEFAULT_MOVER_SLOTS, MIN_MOVER_SLOTS, max);
	}
	
	public static int startupIdentitySlots() {
		int max = startupMaxExtraIdentitySlots();
		return BASE_IDENTITY_SLOTS + readStartupSlotCount("extra_identity_slots", DEFAULT_IDENTITY_SLOTS, MIN_IDENTITY_SLOTS, max);
	}
	
	public static int startupMaxPassiveSlots() {
		return BASE_PASSIVE_SLOTS + startupMaxExtraPassiveSlots();
	}
	
	public static int startupMaxMoverSlots() {
		return BASE_MOVER_SLOTS + startupMaxExtraMoverSlots();
	}
	
	public static int startupMaxIdentitySlots() {
		return BASE_IDENTITY_SLOTS + startupMaxExtraIdentitySlots();
	}
	
	private static int startupMaxExtraPassiveSlots() {
		return readStartupSlotCount("max_extra_passive_slots", DEFAULT_MAX_PASSIVE_SLOTS, MIN_PASSIVE_SLOTS, HARD_MAX_PASSIVE_SLOTS);
	}
	
	private static int startupMaxExtraMoverSlots() {
		return readStartupSlotCount("max_extra_mover_slots", DEFAULT_MAX_MOVER_SLOTS, MIN_MOVER_SLOTS, HARD_MAX_MOVER_SLOTS);
	}
	
	private static int startupMaxExtraIdentitySlots() {
		return readStartupSlotCount("max_extra_identity_slots", DEFAULT_MAX_IDENTITY_SLOTS, MIN_IDENTITY_SLOTS, HARD_MAX_IDENTITY_SLOTS);
	}
	
	private static int readStartupSlotCount(String key, int fallback, int min, int max) {
		Path configPath = FMLPaths.CONFIGDIR.get().resolve(EpicFightSkillExtraSlots.MODID + "-common.toml");
		
		if (!Files.isRegularFile(configPath)) {
			return fallback;
		}
		
		try {
			for (String line : Files.readAllLines(configPath)) {
				Matcher matcher = CONFIG_VALUE.matcher(line);
				
				if (matcher.matches() && key.equals(matcher.group(1))) {
					return clamp(Integer.parseInt(matcher.group(2)), min, max);
				}
			}
		} catch (IOException | NumberFormatException ignored) {
		}
		
		return fallback;
	}
	
	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
	
	public static String key(String suffix) {
		return "gui." + EpicFightSkillExtraSlots.MODID + "." + suffix.toLowerCase(Locale.ROOT);
	}
}
