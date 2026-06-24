package dev.spake404.epicfight.extraslots.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillSlot;

@Mixin(value = CPChangeSkill.class, remap = false)
public interface ExtraSlotsCPChangeSkillAccessor {
	@Accessor("skillSlot")
	SkillSlot epicfight_skill_extraslots$skillSlot();

	@Accessor("skillBookSlotIndex")
	int epicfight_skill_extraslots$skillBookSlotIndex();

	@Accessor("skill")
	Skill epicfight_skill_extraslots$skill();
}
