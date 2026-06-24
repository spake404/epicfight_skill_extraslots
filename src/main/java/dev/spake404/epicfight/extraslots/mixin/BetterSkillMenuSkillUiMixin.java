package dev.spake404.epicfight.extraslots.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.spake404.epicfight.extraslots.ExtraSkillSlots;
import dev.spake404.epicfight.extraslots.ExtraSlotUnlockSkill;
import dev.spake404.epicfight.extraslots.ExtraSlotUnlockSlots;
import org.merlin204.bsm.client.gui.SkillWidget;
import yesman.epicfight.api.utils.ExtendableEnumManager;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillSlot;

@Mixin(targets = "org.merlin204.bsm.client.gui.SkillUI", remap = false)
abstract class BetterSkillMenuSkillUiMixin {
	@Redirect(
		method = "setSlotList(Lyesman/epicfight/client/world/capabilites/entitypatch/player/LocalPlayerPatch;)V",
		at = @At(
			value = "INVOKE",
			target = "Lyesman/epicfight/api/utils/ExtendableEnumManager;universalValues()Ljava/util/Collection;"
		)
	)
	private static Collection<?> epicfight_skill_extraslots$hideDisabledExtraSlots(ExtendableEnumManager<?> manager) {
		return manager.universalValues().stream()
			.filter(value -> !(value instanceof SkillSlot slot) || isVisibleSlot(slot))
			.toList();
	}
	
	@Inject(
		method = "addSkillWidget(Lorg/merlin204/bsm/client/gui/SkillWidget;ILyesman/epicfight/skill/Skill;Lyesman/epicfight/client/world/capabilites/entitypatch/player/LocalPlayerPatch;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void epicfight_skill_extraslots$hideExtraSlotUnlockSkills(SkillWidget widget, int index, Skill skill, LocalPlayerPatch playerPatch, CallbackInfo callback) {
		if (skill instanceof ExtraSlotUnlockSkill) {
			callback.cancel();
		}
	}
	
	private static boolean isVisibleSlot(SkillSlot slot) {
		return !ExtraSlotUnlockSlots.isUnlockSlot(slot) && (!ExtraSkillSlots.isManagedSlot(slot) || ExtraSkillSlots.isEnabled(slot));
	}
}
