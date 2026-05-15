package yesman.epicfight.skill;

public enum SkillCategories implements SkillCategory {
	BASIC_ATTACK,
	DODGE,
	PASSIVE,
	WEAPON_PASSIVE,
	WEAPON_INNATE,
	GUARD,
	KNOCKDOWN_WAKEUP,
	MOVER,
	IDENTITY;
	
	@Override
	public boolean learnable() {
		return true;
	}
	
	@Override
	public int universalOrdinal() {
		return this.ordinal();
	}
}
