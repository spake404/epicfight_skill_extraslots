package dev.spake404.epicfight.extraslots.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import yesman.epicfight.world.capabilities.item.CapabilityItem;

@Mixin(targets = "org.merlin204.bsm.client.gui.SkillInfoWidgetGroup", remap = false)
abstract class BetterSkillMenuRangedCompatMixin {
	@Redirect(
		method = "registerIconItems()V",
		at = @At(
			value = "FIELD",
			target = "Lyesman/epicfight/world/capabilities/item/CapabilityItem$WeaponCategories;RANGED:Lyesman/epicfight/world/capabilities/item/CapabilityItem$WeaponCategories;"
		)
	)
	private static CapabilityItem.WeaponCategories epicfight_skill_extraslots$useBowForRemovedRangedCategory() {
		return CapabilityItem.WeaponCategories.BOW;
	}
}
