package dev.spake404.epicfight.extraslots.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class ExtraSlotsMixinPlugin implements IMixinConfigPlugin {
	private static final String BETTER_SKILL_MENU_RANGED_COMPAT = "dev.spake404.epicfight.extraslots.mixin.BetterSkillMenuRangedCompatMixin";
	
	@Override
	public void onLoad(String mixinPackage) {
	}
	
	@Override
	public String getRefMapperConfig() {
		return null;
	}
	
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (BETTER_SKILL_MENU_RANGED_COMPAT.equals(mixinClassName)) {
			return !epicFightHasRangedWeaponCategory();
		}
		
		return true;
	}
	
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}
	
	@Override
	public List<String> getMixins() {
		return null;
	}
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
	
	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
	
	private static boolean epicFightHasRangedWeaponCategory() {
		try {
			Class<?> weaponCategories = Class.forName("yesman.epicfight.world.capabilities.item.CapabilityItem$WeaponCategories", false, ExtraSlotsMixinPlugin.class.getClassLoader());
			weaponCategories.getField("RANGED");
			return true;
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return false;
		}
	}
	
}
