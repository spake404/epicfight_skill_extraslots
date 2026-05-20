package dev.spake404.epicfight.extraslots;

import java.util.Set;

import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.world.capability.SkillTreeProgression;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

final class ExtraSlotsSkillTreeHooks {
	private static final ResourceKey<SkillTree> EXTRA_SLOT_TREE = ResourceKey.create(
		SkillTree.SKILL_TREE_REGISTRY_KEY,
		ResourceLocation.fromNamespaceAndPath(EpicFightSkillExtraSlots.MODID, ExtraSlotsSkillTreeCompat.TREE_NAME)
	);
	
	private ExtraSlotsSkillTreeHooks() {
	}
	
	static ExtraSlotCounts countsFor(ServerPlayer player) {
		int[] counts = {
			ExtraSlotsConfig.BASE_PASSIVE_SLOTS,
			ExtraSlotsConfig.BASE_MOVER_SLOTS,
			ExtraSlotsConfig.BASE_IDENTITY_SLOTS
		};
		
		player.getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(progression -> {
			Holder.Reference<SkillTree> tree;
			CapabilitySkill skills = EpicFightCapabilities.getPlayerPatch(player) == null ? null : EpicFightCapabilities.getPlayerPatch(player).getSkillCapability();
			
			try {
				tree = player.server.registryAccess().registryOrThrow(SkillTree.SKILL_TREE_REGISTRY_KEY).getHolderOrThrow(EXTRA_SLOT_TREE);
			} catch (RuntimeException ignored) {
				return;
			}
			
			counts[0] = countUnlocked(progression, tree, ExtraSlotsSkillTreeCompat.SlotGroup.PASSIVE, skills);
			counts[1] = countUnlocked(progression, tree, ExtraSlotsSkillTreeCompat.SlotGroup.MOVER, skills);
			counts[2] = countUnlocked(progression, tree, ExtraSlotsSkillTreeCompat.SlotGroup.IDENTITY, skills);
		});
		
		return new ExtraSlotCounts(counts[0], counts[1], counts[2]);
	}
	
	private static int countUnlocked(SkillTreeProgression progression, Holder.Reference<SkillTree> tree, ExtraSlotsSkillTreeCompat.SlotGroup group, CapabilitySkill skills) {
		int count = group.baseSlots();
		
		for (int slotIndex = group.baseSlots() + 1; slotIndex <= group.startupMaxSlots(); slotIndex++) {
			Skill skill = ExtraSlotsSkillTreeCompat.skill(group, slotIndex);
			
			if (skill != null) {
				try {
					if (progression.getNodeState(tree, skill) == SkillTreeProgression.NodeState.UNLOCKED && group.isEquipped(skills, skill)) {
						count = slotIndex;
					} else {
						break;
					}
				} catch (RuntimeException ignored) {
					return count;
				}
			}
		}
		
		return count;
	}
	
	static VerificationResult verifySoulStoneCosts(ServerPlayer player, Set<String> confirmedUnlocks, boolean firstScan) {
		VerificationResult result = new VerificationResult();
		
		player.getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(progression -> {
			Holder.Reference<SkillTree> tree;
			CapabilitySkill skills = EpicFightCapabilities.getPlayerPatch(player) == null ? null : EpicFightCapabilities.getPlayerPatch(player).getSkillCapability();
			
			try {
				tree = player.server.registryAccess().registryOrThrow(SkillTree.SKILL_TREE_REGISTRY_KEY).getHolderOrThrow(EXTRA_SLOT_TREE);
			} catch (RuntimeException ignored) {
				return;
			}
			
			for (ExtraSlotsSkillTreeCompat.SlotGroup group : ExtraSlotsSkillTreeCompat.SlotGroup.values()) {
				for (int slotIndex = group.baseSlots() + 1; slotIndex <= group.startupMaxSlots(); slotIndex++) {
					Skill skill = ExtraSlotsSkillTreeCompat.skill(group, slotIndex);
					
					if (skill == null) {
						continue;
					}
					
					String key = ExtraSlotsSkillTreeCompat.unlockKey(group, slotIndex);
					SkillTreeProgression.NodeState state;
					
					try {
						state = progression.getNodeState(tree, skill);
					} catch (RuntimeException ignored) {
						continue;
					}
					
					if (state != SkillTreeProgression.NodeState.UNLOCKED) {
						confirmedUnlocks.remove(key);
						continue;
					}
					
					if (!group.isEquipped(skills, skill)) {
						confirmedUnlocks.remove(key);
						progression.lockNode(EXTRA_SLOT_TREE, skill, true, player);
						result.slotsChanged = true;
						continue;
					}
					
					if (confirmedUnlocks.contains(key)) {
						continue;
					}
					
					if (firstScan) {
						confirmedUnlocks.add(key);
						continue;
					}
					
					if (ExtraSlotsSkillTreeCompat.consumeSoulStone(player, group)) {
						confirmedUnlocks.add(key);
						result.slotsChanged = true;
						result.soulStonesChanged = true;
					} else {
						progression.lockNode(EXTRA_SLOT_TREE, skill, true, player);
						result.slotsChanged = true;
						result.soulStonesChanged = true;
						player.sendSystemMessage(Component.translatable("message." + EpicFightSkillExtraSlots.MODID + ".missing_soul_stone", group.soulStone().get().getDescription()));
					}
				}
			}
		});
		
		return result;
	}
	
	static final class VerificationResult {
		private boolean slotsChanged;
		private boolean soulStonesChanged;
		
		boolean slotsChanged() {
			return this.slotsChanged;
		}
		
		boolean soulStonesChanged() {
			return this.soulStonesChanged;
		}
	}
	
}
