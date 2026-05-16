package dev.spake404.epicfight.extraslots;

import java.util.Collection;
import java.util.function.IntSupplier;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

public final class ExtraSlotsApi {
	private ExtraSlotsApi() {
	}
	
	public static Result add(Collection<? extends ServerPlayer> players, SlotGroup group, int amount) {
		return change(players, group, Math.max(0, amount));
	}
	
	public static Result remove(Collection<? extends ServerPlayer> players, SlotGroup group, int amount) {
		return change(players, group, -Math.max(0, amount));
	}
	
	public static Result set(Collection<? extends ServerPlayer> players, SlotGroup group, int count) {
		int current = group.get();
		int next = clamp(count, group.min(), group.max());
		return apply(players, group, current, next);
	}
	
	public static int get(SlotGroup group) {
		return group.get();
	}
	
	private static Result change(Collection<? extends ServerPlayer> players, SlotGroup group, int delta) {
		int current = group.get();
		int next = clamp(current + delta, group.min(), group.max());
		return apply(players, group, current, next);
	}
	
	private static Result apply(Collection<? extends ServerPlayer> players, SlotGroup group, int previous, int next) {
		group.value().set(next);
		ExtraSlotsConfig.SPEC.save();
		ExtraSkillSlots.applyConfiguredSlots();
		
		int appliedPlayers = 0;
		
		for (ServerPlayer player : players) {
			ExtraSlotsRuntimeExpander.expandAndClean(EpicFightCapabilities.getPlayerPatch(player));
			ExtraSlotsNetwork.sync(player);
			appliedPlayers++;
		}
		
		return new Result(group, previous, next, appliedPlayers);
	}
	
	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
	
	public enum SlotGroup {
		PASSIVE("Passive", SkillCategories.PASSIVE, ExtraSlotsConfig.PASSIVE_SLOTS, ExtraSlotsConfig.MIN_PASSIVE_SLOTS, ExtraSlotsConfig::maxPassiveSlots),
		MOVER("Mover", SkillCategories.MOVER, ExtraSlotsConfig.MOVER_SLOTS, ExtraSlotsConfig.MIN_MOVER_SLOTS, ExtraSlotsConfig::maxMoverSlots),
		IDENTITY("Identity", SkillCategories.IDENTITY, ExtraSlotsConfig.IDENTITY_SLOTS, ExtraSlotsConfig.MIN_IDENTITY_SLOTS, ExtraSlotsConfig::maxIdentitySlots);
		
		private final String displayName;
		private final SkillCategory skillCategory;
		private final IntValue value;
		private final int min;
		private final IntSupplier max;
		
		SlotGroup(String displayName, SkillCategory skillCategory, IntValue value, int min, IntSupplier max) {
			this.displayName = displayName;
			this.skillCategory = skillCategory;
			this.value = value;
			this.min = min;
			this.max = max;
		}
		
		public String displayName() {
			return this.displayName;
		}
		
		public SkillCategory skillCategory() {
			return this.skillCategory;
		}
		
		public int get() {
			return this.value.get();
		}
		
		public int min() {
			return this.min;
		}
		
		public int max() {
			return this.max.getAsInt();
		}
		
		private IntValue value() {
			return this.value;
		}
	}
	
	public record Result(SlotGroup group, int previous, int current, int appliedPlayers) {
		public int changedBy() {
			return this.current - this.previous;
		}
		
		public boolean unchanged() {
			return this.previous == this.current;
		}
		
		public boolean atMinimum() {
			return this.current <= this.group.min();
		}
		
		public boolean atMaximum() {
			return this.current >= this.group.max();
		}
	}
}
