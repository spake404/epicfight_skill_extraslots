package yesman.epicfight.skill;

public class Skill {
	public Skill(SkillBuilder<? extends Skill> builder) {
	}
	
	public static SkillBuilder<Skill> createBuilder() {
		return new SkillBuilder<>();
	}
	
	public SkillCategory getCategory() {
		return null;
	}
	
	public String getTranslationKey() {
		return "";
	}
	
	public Object getSkillTexture() {
		return null;
	}
	
	public enum ActivateType {
		ONE_SHOT
	}
	
	public enum Resource {
		NONE
	}
}
