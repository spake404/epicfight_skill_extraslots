package dev.spake404.epicfight.extraslots.mixin;

import dev.spake404.epicfight.extraslots.ExtraSlotsMutualExclusionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;

@Mixin(targets = "com.yesman.epicskills.client.gui.screen.SkillInfoScreen", remap = false)
abstract class ExtraSlotsSkillInfoScreenMixin extends SkillBookScreen {
	private ExtraSlotsSkillInfoScreenMixin(Player opener, Skill skill, InteractionHand hand, Screen parentScreen) {
		super(opener, skill, hand, parentScreen);
	}

	@Inject(
		method = "acquireSkillTo(Lyesman/epicfight/skill/SkillContainer;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void epicfight_skill_extraslots$confirmSkillTreeMutualExclusionReplace(SkillContainer container, CallbackInfo callback) {
		boolean prompted = ExtraSlotsMutualExclusionClient.promptIfNeeded(
			(Screen)(Object)this,
			this.playerpatch.getSkillCapability(),
			container,
			this.skill,
			-1,
			() -> Minecraft.getInstance().setScreen(this.parentScreen)
		);

		if (prompted) {
			callback.cancel();
		}
	}
}
