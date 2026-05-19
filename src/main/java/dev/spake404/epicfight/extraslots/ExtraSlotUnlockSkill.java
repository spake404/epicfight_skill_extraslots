package dev.spake404.epicfight.extraslots;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public class ExtraSlotUnlockSkill extends Skill {
	private final ExtraSlotsSkillTreeCompat.SlotGroup group;
	private final int slotIndex;
	
	public ExtraSlotUnlockSkill(SkillBuilder<? extends Skill> builder, ExtraSlotsSkillTreeCompat.SlotGroup group, int slotIndex) {
		super(builder);
		this.group = group;
		this.slotIndex = slotIndex;
	}
	
	public ExtraSlotsSkillTreeCompat.SlotGroup group() {
		return this.group;
	}
	
	public int slotIndex() {
		return this.slotIndex;
	}
	
	@Override
	public String getTranslationKey() {
		return "skill." + EpicFightSkillExtraSlots.MODID + "." + this.group.skillName(this.slotIndex);
	}
	
	@Override
	public ResourceLocation getSkillTexture() {
		return this.group.icon();
	}
	
	public List<Component> getTooltipOnItem(ItemStack stack, CapabilityItem itemCap, PlayerPatch<?> playerPatch) {
		return List.of(Component.translatable(this.getTranslationKey() + ".tooltip"));
	}
}
