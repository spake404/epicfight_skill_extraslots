package yesman.epicfight.skill;

import yesman.epicfight.api.utils.ExtendableEnum;
import yesman.epicfight.api.utils.ExtendableEnumManager;

public interface SkillCategory extends ExtendableEnum {
	ExtendableEnumManager<SkillCategory> ENUM_MANAGER = new ExtendableEnumManager<>();
	
	default boolean shouldSave() {
		return true;
	}
	
	default boolean shouldSynchronize() {
		return true;
	}
	
	boolean learnable();
	
	default Object bookIcon() {
		return null;
	}
}
