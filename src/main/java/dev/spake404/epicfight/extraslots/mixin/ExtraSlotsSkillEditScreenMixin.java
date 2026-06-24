package dev.spake404.epicfight.extraslots.mixin;

import dev.spake404.epicfight.extraslots.ExtraSlotsMutualExclusionClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

@Mixin(targets = "yesman.epicfight.client.gui.screen.SkillEditScreen", remap = false)
abstract class ExtraSlotsSkillEditScreenMixin {
	@Shadow
	@Final
	private CapabilitySkill skills;

	@Inject(
		method = "lambda$init$5(Lyesman/epicfight/skill/SkillContainer;Lyesman/epicfight/skill/Skill;Lyesman/epicfight/skill/SkillSlot;Lnet/minecraft/client/gui/components/Button;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void epicfight_skill_extraslots$confirmMutualExclusionReplace(SkillContainer container, Skill skill, SkillSlot slot, Button button, CallbackInfo callback) {
		boolean prompted = ExtraSlotsMutualExclusionClient.promptIfNeeded(
			(Screen)(Object)this,
			this.skills,
			container,
			skill,
			-1,
			() -> ((Screen)(Object)this).onClose()
		);

		if (prompted) {
			callback.cancel();
		}
	}
}
