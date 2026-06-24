package dev.spake404.epicfight.extraslots;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import yesman.epicfight.client.gui.datapack.screen.MessageScreen;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

public final class ExtraSlotsMutualExclusionClient {
	private ExtraSlotsMutualExclusionClient() {
	}

	public static boolean promptIfNeeded(Screen source, CapabilitySkill skills, SkillContainer target, Skill incoming, int skillBookSlotIndex, Runnable closeAfterConfirm) {
		ExtraSlotsMutualExclusions.Conflict conflict = ExtraSlotsMutualExclusions.findConflict(skills, incoming, target).orElse(null);
		if (conflict == null) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Component conflictName = conflict.skill().getDisplayName();
		Component message = Component.translatable("gui." + EpicFightSkillExtraSlots.MODID + ".mutual_exclusion.message", conflictName, conflictName);
		MessageScreen<Void> screen = new MessageScreen<>(
			"",
			message,
			source,
			button -> {
				conflict.container().setSkill(null);
				target.setSkill(incoming);
				skills.addLearnedSkill(incoming);
				ExtraSlotsNetwork.sendConfirmedSkillChange(target.getSlot(), skillBookSlotIndex, incoming);
				closeAfterConfirm.run();
			},
			button -> minecraft.setScreen(source),
			220,
			0
		);
		minecraft.setScreen(screen.setLayerFarPlane(2000).autoCalculateHeight());
		return true;
	}
}
