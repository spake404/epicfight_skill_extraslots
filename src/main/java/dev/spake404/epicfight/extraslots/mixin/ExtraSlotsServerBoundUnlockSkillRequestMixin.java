package dev.spake404.epicfight.extraslots.mixin;

import com.yesman.epicskills.network.server.ServerBoundUnlockSkillRequest;
import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.world.capability.AbilityPoints;
import com.yesman.epicskills.world.capability.SkillTreeProgression;

import dev.spake404.epicfight.extraslots.EpicFightSkillExtraSlots;
import dev.spake404.epicfight.extraslots.ExtraSlotUnlockSkill;
import dev.spake404.epicfight.extraslots.ExtraSlotsMutualExclusions;
import dev.spake404.epicfight.extraslots.ExtraSlotsNetwork;
import dev.spake404.epicfight.extraslots.ExtraSlotsRuntimeExpander;
import dev.spake404.epicfight.extraslots.ExtraSlotsSkillTreeCompat;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@Mixin(value = ServerBoundUnlockSkillRequest.class, remap = false)
abstract class ExtraSlotsServerBoundUnlockSkillRequestMixin {
	@Inject(
		method = "lambda$handle$2(Lcom/yesman/epicskills/network/server/ServerBoundUnlockSkillRequest;Lnet/minecraft/server/level/ServerPlayer;Lorg/apache/commons/lang3/mutable/MutableBoolean;Lyesman/epicfight/world/capabilities/entitypatch/player/ServerPlayerPatch;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void epicfight_skill_extraslots$askChangeInsteadOfAutoEquippingConflict(
		ServerBoundUnlockSkillRequest request,
		ServerPlayer player,
		MutableBoolean askChange,
		ServerPlayerPatch playerPatch,
		CallbackInfo callback
	) {
		Skill incoming = request.skill();
		if (incoming == null || playerPatch.getSkillCapability().isEquipping(incoming)) {
			return;
		}

		if (ExtraSlotsMutualExclusions.findConflict(playerPatch.getSkillCapability(), incoming, null).isPresent()) {
			askChange.setTrue();
			callback.cancel();
		}
	}

	@Inject(
		method = "lambda$handle$3(Lcom/yesman/epicskills/world/capability/SkillTreeProgression;Lcom/yesman/epicskills/network/server/ServerBoundUnlockSkillRequest;Lcom/yesman/epicskills/world/capability/AbilityPoints;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/Holder$Reference;)V",
		at = @At(
			value = "INVOKE",
			target = "Lcom/yesman/epicskills/world/capability/SkillTreeProgression;unlockNode(Lnet/minecraft/core/Holder$Reference;Lyesman/epicfight/skill/Skill;)V"
		),
		cancellable = true
	)
	private static void epicfight_skill_extraslots$consumeSoulStoneBeforeUnlock(
		SkillTreeProgression progression,
		ServerBoundUnlockSkillRequest request,
		AbilityPoints abilityPoints,
		ServerPlayer player,
		Holder.Reference<SkillTree> skillTree,
		CallbackInfo callback
	) {
		if (!(request.skill() instanceof ExtraSlotUnlockSkill skill)) {
			return;
		}

		if (ExtraSlotsSkillTreeCompat.isConfirmedUnlock(player, skill.group(), skill.slotIndex())) {
			return;
		}

		if (ExtraSlotsSkillTreeCompat.consumeSoulStone(player, skill.group())) {
			ExtraSlotsSkillTreeCompat.confirmUnlock(player, skill.group(), skill.slotIndex());
			ExtraSlotsRuntimeExpander.expand(EpicFightCapabilities.getPlayerPatch(player), ExtraSlotsSkillTreeCompat.activeCounts(player));
			return;
		}

		ExtraSlotsNetwork.syncSoulStones(player);
		player.sendSystemMessage(Component.translatable("message." + EpicFightSkillExtraSlots.MODID + ".missing_soul_stone", skill.group().soulStone().get().getDescription()));
		abilityPoints.sendChanges();
		callback.cancel();
	}

	@Inject(
		method = "lambda$handle$3(Lcom/yesman/epicskills/world/capability/SkillTreeProgression;Lcom/yesman/epicskills/network/server/ServerBoundUnlockSkillRequest;Lcom/yesman/epicskills/world/capability/AbilityPoints;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/Holder$Reference;)V",
		at = @At("RETURN")
	)
	private static void epicfight_skill_extraslots$syncAfterExtraSlotUnlock(
		SkillTreeProgression progression,
		ServerBoundUnlockSkillRequest request,
		AbilityPoints abilityPoints,
		ServerPlayer player,
		Holder.Reference<SkillTree> skillTree,
		CallbackInfo callback
	) {
		if (!(request.skill() instanceof ExtraSlotUnlockSkill skill) || !ExtraSlotsSkillTreeCompat.isConfirmedUnlock(player, skill.group(), skill.slotIndex())) {
			return;
		}

		PlayerPatch<?> playerPatch = EpicFightCapabilities.getPlayerPatch(player);
		if (playerPatch != null) {
			ExtraSlotsRuntimeExpander.moveUnlockMarkerSkillsToHiddenSlots(playerPatch.getSkillCapability(), player);
			ExtraSlotsRuntimeExpander.expandAndClean(playerPatch, ExtraSlotsSkillTreeCompat.activeCounts(player), player);
		}

		ExtraSlotsNetwork.sync(player);
		ExtraSlotsNetwork.syncSoulStones(player);
	}
}
