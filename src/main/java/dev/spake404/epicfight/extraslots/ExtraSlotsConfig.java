package dev.spake404.epicfight.extraslots;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
	public static final int MIN_PASSIVE_SLOTS = BASE_PASSIVE_SLOTS;
	public static final int MIN_MOVER_SLOTS = BASE_MOVER_SLOTS;
	public static final int MIN_IDENTITY_SLOTS = BASE_IDENTITY_SLOTS;
	public static final int DEFAULT_PASSIVE_SLOTS = 3;
	public static final int DEFAULT_MOVER_SLOTS = 1;
	public static final int DEFAULT_IDENTITY_SLOTS = 1;
	public static final int DEFAULT_MAX_PASSIVE_SLOTS = 11;
	public static final int DEFAULT_MAX_MOVER_SLOTS = 9;
	public static final int DEFAULT_MAX_IDENTITY_SLOTS = 9;
	public static final int HARD_MAX_PASSIVE_SLOTS = 64;
	public static final int HARD_MAX_MOVER_SLOTS = 64;
	public static final int HARD_MAX_IDENTITY_SLOTS = 64;
	
	private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	
	public static final ForgeConfigSpec.IntValue MAX_PASSIVE_SLOTS;
	public static final ForgeConfigSpec.IntValue MAX_MOVER_SLOTS;
	public static final ForgeConfigSpec.IntValue MAX_IDENTITY_SLOTS;
	public static final ForgeConfigSpec.IntValue PASSIVE_SLOTS;
	public static final ForgeConfigSpec.IntValue MOVER_SLOTS;
	public static final ForgeConfigSpec.IntValue IDENTITY_SLOTS;
	public static final ForgeConfigSpec SPEC;
	
	static {
		BUILDER.push("skill_slots");
		MAX_PASSIVE_SLOTS = BUILDER.comment("Maximum configurable passive skill slots, including Epic Fight's built-in passive slots.")
			.defineInRange("max_passive_slots", DEFAULT_MAX_PASSIVE_SLOTS, MIN_PASSIVE_SLOTS, HARD_MAX_PASSIVE_SLOTS);
		MAX_MOVER_SLOTS = BUILDER.comment("Maximum configurable mover skill slots, including Epic Fight's built-in mover slot.")
			.defineInRange("max_mover_slots", DEFAULT_MAX_MOVER_SLOTS, MIN_MOVER_SLOTS, HARD_MAX_MOVER_SLOTS);
		MAX_IDENTITY_SLOTS = BUILDER.comment("Maximum configurable identity skill slots, including Epic Fight's built-in identity slot.")
			.defineInRange("max_identity_slots", DEFAULT_MAX_IDENTITY_SLOTS, MIN_IDENTITY_SLOTS, HARD_MAX_IDENTITY_SLOTS);
		PASSIVE_SLOTS = BUILDER.comment("Total passive skill slots after the next game restart, including Epic Fight's built-in passive slots.")
			.defineInRange("passive_slots", DEFAULT_PASSIVE_SLOTS, MIN_PASSIVE_SLOTS, HARD_MAX_PASSIVE_SLOTS);
		MOVER_SLOTS = BUILDER.comment("Total mover skill slots after the next game restart, including Epic Fight's built-in mover slot.")
			.defineInRange("mover_slots", DEFAULT_MOVER_SLOTS, MIN_MOVER_SLOTS, HARD_MAX_MOVER_SLOTS);
		IDENTITY_SLOTS = BUILDER.comment("Total identity skill slots after the next game restart, including Epic Fight's built-in identity slot.")
			.defineInRange("identity_slots", DEFAULT_IDENTITY_SLOTS, MIN_IDENTITY_SLOTS, HARD_MAX_IDENTITY_SLOTS);
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
		return clamp(PASSIVE_SLOTS.get(), MIN_PASSIVE_SLOTS, maxPassiveSlots());
	}
	
	public static int moverSlots() {
		return clamp(MOVER_SLOTS.get(), MIN_MOVER_SLOTS, maxMoverSlots());
	}
	
	public static int identitySlots() {
		return clamp(IDENTITY_SLOTS.get(), MIN_IDENTITY_SLOTS, maxIdentitySlots());
	}
	
	public static int maxPassiveSlots() {
		return MAX_PASSIVE_SLOTS.get();
	}
	
	public static int maxMoverSlots() {
		return MAX_MOVER_SLOTS.get();
	}
	
	public static int maxIdentitySlots() {
		return MAX_IDENTITY_SLOTS.get();
	}
	
	public static int startupPassiveSlots() {
		int max = startupMaxPassiveSlots();
		return readStartupSlotCount("passive_slots", DEFAULT_PASSIVE_SLOTS, MIN_PASSIVE_SLOTS, max);
	}
	
	public static int startupMoverSlots() {
		int max = startupMaxMoverSlots();
		return readStartupSlotCount("mover_slots", DEFAULT_MOVER_SLOTS, MIN_MOVER_SLOTS, max);
	}
	
	public static int startupIdentitySlots() {
		int max = startupMaxIdentitySlots();
		return readStartupSlotCount("identity_slots", DEFAULT_IDENTITY_SLOTS, MIN_IDENTITY_SLOTS, max);
	}
	
	public static int startupMaxPassiveSlots() {
		return readStartupSlotCount("max_passive_slots", DEFAULT_MAX_PASSIVE_SLOTS, MIN_PASSIVE_SLOTS, HARD_MAX_PASSIVE_SLOTS);
	}
	
	public static int startupMaxMoverSlots() {
		return readStartupSlotCount("max_mover_slots", DEFAULT_MAX_MOVER_SLOTS, MIN_MOVER_SLOTS, HARD_MAX_MOVER_SLOTS);
	}
	
	public static int startupMaxIdentitySlots() {
		return readStartupSlotCount("max_identity_slots", DEFAULT_MAX_IDENTITY_SLOTS, MIN_IDENTITY_SLOTS, HARD_MAX_IDENTITY_SLOTS);
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
