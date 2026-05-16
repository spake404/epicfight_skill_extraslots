package yesman.epicfight.skill;

import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class SkillContainer {
	public SkillContainer(PlayerPatch<?> playerPatch, SkillSlot slot) {
	}
	
	public SkillSlot getSlot() {
		return null;
	}
	
	public boolean isEmpty() {
		return true;
	}
	
	public Skill getSkill() {
		return null;
	}
	
	public boolean setSkill(Skill skill) {
		return true;
	}
}
