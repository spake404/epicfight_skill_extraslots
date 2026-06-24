package dev.spake404.epicfight.extraslots.mixin;

import dev.spake404.epicfight.extraslots.ExtraSlotsMutualExclusions;
import dev.spake404.epicfight.extraslots.ExtraSlotsNetwork;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

@Mixin(value = CPChangeSkill.class, remap = false)
abstract class ExtraSlotsCPChangeSkillMixin {
	@Inject(
		method = "lambda$handle$0(Lyesman/epicfight/network/client/CPChangeSkill;Lyesman/epicfight/world/capabilities/entitypatch/player/ServerPlayerPatch;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void epicfight_skill_extraslots$blockUnconfirmedMutualExclusionReplace(CPChangeSkill packet, ServerPlayerPatch playerPatch, CallbackInfo callback) {
		ExtraSlotsCPChangeSkillAccessor accessor = (ExtraSlotsCPChangeSkillAccessor)packet;
		Skill incoming = accessor.epicfight_skill_extraslots$skill();
		SkillSlot slot = accessor.epicfight_skill_extraslots$skillSlot();
		if (incoming == null || slot == null) {
			return;
		}

		SkillContainer target = playerPatch.getSkill(slot);
		ExtraSlotsMutualExclusions.Conflict conflict = ExtraSlotsMutualExclusions.findConflict(playerPatch.getSkillCapability(), incoming, target).orElse(null);
		if (conflict == null) {
			return;
		}

		ServerPlayer player = (ServerPlayer)playerPatch.getOriginal();
		ExtraSlotsNetwork.syncSkillContainer(player, conflict.container());
		ExtraSlotsNetwork.syncSkillContainer(player, target);
		callback.cancel();
	}
}
