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
	public static final int MAX_PASSIVE_SLOTS = 11;
	public static final int MAX_MOVER_SLOTS = 9;
	public static final int MAX_IDENTITY_SLOTS = 9;
	
	private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	
	public static final ForgeConfigSpec.IntValue PASSIVE_SLOTS;
	public static final ForgeConfigSpec.IntValue MOVER_SLOTS;
	public static final ForgeConfigSpec.IntValue IDENTITY_SLOTS;
	public static final ForgeConfigSpec SPEC;
	
	static {
		BUILDER.push("skill_slots");
		PASSIVE_SLOTS = BUILDER.comment("Total passive skill slots after the next game restart, including Epic Fight's built-in passive slots.")
			.defineInRange("passive_slots", DEFAULT_PASSIVE_SLOTS, MIN_PASSIVE_SLOTS, MAX_PASSIVE_SLOTS);
		MOVER_SLOTS = BUILDER.comment("Total mobility/action skill slots after the next game restart, including Epic Fight's built-in mobility slot.")
			.defineInRange("mover_slots", DEFAULT_MOVER_SLOTS, MIN_MOVER_SLOTS, MAX_MOVER_SLOTS);
		IDENTITY_SLOTS = BUILDER.comment("Total identity skill slots after the next game restart, including Epic Fight's built-in identity slot.")
			.defineInRange("identity_slots", DEFAULT_IDENTITY_SLOTS, MIN_IDENTITY_SLOTS, MAX_IDENTITY_SLOTS);
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
		return PASSIVE_SLOTS.get();
	}
	
	public static int moverSlots() {
		return MOVER_SLOTS.get();
	}
	
	public static int identitySlots() {
		return IDENTITY_SLOTS.get();
	}
	
	public static int startupPassiveSlots() {
		return readStartupSlotCount("passive_slots", DEFAULT_PASSIVE_SLOTS, MIN_PASSIVE_SLOTS, MAX_PASSIVE_SLOTS);
	}
	
	public static int startupMoverSlots() {
		return readStartupSlotCount("mover_slots", DEFAULT_MOVER_SLOTS, MIN_MOVER_SLOTS, MAX_MOVER_SLOTS);
	}
	
	public static int startupIdentitySlots() {
		return readStartupSlotCount("identity_slots", DEFAULT_IDENTITY_SLOTS, MIN_IDENTITY_SLOTS, MAX_IDENTITY_SLOTS);
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
