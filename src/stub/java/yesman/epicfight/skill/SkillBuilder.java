package yesman.epicfight.skill;

public class SkillBuilder<T extends Skill> {
	public <B extends SkillBuilder<T>> B setRegistryName(Object registryName) {
		return (B)this;
	}
	
	public <B extends SkillBuilder<T>> B setCreativeTab(Object creativeTab) {
		return (B)this;
	}
	
	public <B extends SkillBuilder<T>> B setCategory(SkillCategory category) {
		return (B)this;
	}
	
	public <B extends SkillBuilder<T>> B setActivateType(Skill.ActivateType activateType) {
		return (B)this;
	}
	
	public <B extends SkillBuilder<T>> B setResource(Skill.Resource resource) {
		return (B)this;
	}
}
