package yesman.epicfight.skill;

import yesman.epicfight.api.utils.ExtendableEnum;
import yesman.epicfight.api.utils.ExtendableEnumManager;

public interface SkillSlot extends ExtendableEnum {
	ExtendableEnumManager<SkillSlot> ENUM_MANAGER = new ExtendableEnumManager<>();
	
	SkillCategory category();
}
