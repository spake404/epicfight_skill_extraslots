package yesman.epicfight.world.capabilities.skill;

import java.util.Set;
import java.util.stream.Stream;

import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;

public class CapabilitySkill {
	public Set<SkillContainer> getSkillContainersFor(SkillCategory category) {
		return Set.of();
	}
	
	public SkillContainer getSkillContainer(Skill skill) {
		return null;
	}
	
	public Stream<SkillContainer> listSkillContainers() {
		return Stream.empty();
	}
}
