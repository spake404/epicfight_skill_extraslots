package dev.spake404.epicfight.extraslots.mixin;

import dev.spake404.epicfight.extraslots.ExtraSlotsMutualExclusionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;

@Mixin(targets = "yesman.epicfight.client.gui.screen.SkillBookScreen", remap = false)
abstract class ExtraSlotsSkillBookScreenMixin {
	@Shadow
	@Final
	protected Player opener;

	@Shadow
	@Final
	protected LocalPlayerPatch playerpatch;

	@Shadow
	@Final
	protected Skill skill;

	@Shadow
	@Final
	protected InteractionHand hand;

	@Inject(
		method = "acquireSkillTo(Lyesman/epicfight/skill/SkillContainer;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void epicfight_skill_extraslots$confirmMutualExclusionReplace(SkillContainer container, CallbackInfo callback) {
		int skillBookSlotIndex = this.hand == InteractionHand.MAIN_HAND ? this.opener.getInventory().selected : 40;
		boolean prompted = ExtraSlotsMutualExclusionClient.promptIfNeeded(
			(Screen)(Object)this,
			this.playerpatch.getSkillCapability(),
			container,
			this.skill,
			skillBookSlotIndex,
			() -> Minecraft.getInstance().setScreen(null)
		);

		if (prompted) {
			callback.cancel();
		}
	}
}
